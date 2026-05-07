import { existsSync, readFileSync } from 'fs';
import { join } from 'path';
import cors from 'cors';
import express, { type NextFunction, type Request, type Response } from 'express';
import swaggerUi from 'swagger-ui-express';
import Rails from '@railsinfra/rails-typescript';
import { loadConfig } from './config';
import { createInsecureAwareProxyFetch, logTlsMode } from './fetch-proxy';
import { toErrorBody } from './errors';
import { registerRoutes } from './routes';

function loadOpenApiSpec(): Record<string, unknown> {
  const openapiPath = join(__dirname, '..', '..', 'kotlin', 'src', 'main', 'resources', 'openapi.json');
  if (!existsSync(openapiPath)) {
    throw new Error(
      `OpenAPI file not found at ${openapiPath}. Ensure src/rails-sdks/samples/kotlin is present in the repo.`,
    );
  }
  const raw = JSON.parse(readFileSync(openapiPath, 'utf8')) as Record<string, unknown>;
  const info = { ...(raw.info as object), title: 'Rails TypeScript SDK sample' };
  return { ...raw, info };
}

function main(): void {
  const cfg = loadConfig();
  logTlsMode(cfg.insecureProxyTls);
  const proxyFetch = createInsecureAwareProxyFetch(cfg.insecureProxyTls);

  const client = new Rails({
    apiKey: cfg.apiKey,
    baseURL: cfg.baseURL,
    defaultHeaders: { 'X-Environment': 'sandbox' },
  });

  const app = express();
  app.use(cors());
  app.use(express.json({ limit: '2mb' }));

  const openapiSpec = loadOpenApiSpec();

  app.get('/health', (_req: Request, res: Response) => {
    res.type('application/json').send(JSON.stringify({ status: 'ok' }));
  });

  registerRoutes(app, {
    baseURL: cfg.baseURL,
    apiKey: cfg.apiKey,
    client,
    proxyFetch,
  });

  app.get('/openapi.json', (_req: Request, res: Response) => {
    res.type('application/json').send(JSON.stringify(openapiSpec, null, 2));
  });

  app.use('/', swaggerUi.serve);
  app.get('/', swaggerUi.setup(openapiSpec));

  app.use((err: unknown, req: Request, res: Response, _next: NextFunction) => {
    if (res.headersSent) return;
    console.error(err);
    const path = req.path ?? '';
    const body = toErrorBody(err, path);
    const status = body.status >= 400 && body.status < 600 ? body.status : 500;
    res.status(status).json(body);
  });

  app.listen(cfg.port, () => {
    // eslint-disable-next-line no-console
    console.error(
      `[rails-sdk-sample] listening on http://localhost:${cfg.port} — Swagger UI at /, OpenAPI at /openapi.json`,
    );
  });
}

main();
