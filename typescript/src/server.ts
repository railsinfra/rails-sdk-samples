import { existsSync, readFileSync } from 'fs';
import { join } from 'path';
import cors from 'cors';
import express, { type NextFunction, type Request, type Response } from 'express';
import swaggerUi from 'swagger-ui-express';
import Rails from '@railsinfra/rails';
import { loadConfig } from './config';
import { createInsecureAwareProxyFetch, logTlsMode } from './fetch-proxy';
import { toErrorBody } from './errors';
import { registerRoutes } from './routes';

function loadOpenApiSpec(): Record<string, unknown> {
  const openapiPath = join(__dirname, '..', '..', 'kotlin', 'src', 'main', 'resources', 'openapi.json');
  if (!existsSync(openapiPath)) {
    throw new Error(
      `OpenAPI file not found at ${openapiPath}. Ensure kotlin/src/main/resources/openapi.json is present in this repo.`,
    );
  }
  const raw = JSON.parse(readFileSync(openapiPath, 'utf8')) as Record<string, unknown>;
  const info = { ...(raw.info as object), title: 'Rails TypeScript SDK sample' };
  const spec = { ...raw, info } as Record<string, any>;
  spec.tags = [
    ...((spec.tags as Array<Record<string, unknown>> | undefined) ?? []),
    { name: 'Audit', description: 'SDK audit events' },
  ];
  spec.paths = {
    ...(spec.paths ?? {}),
    '/api/v1/audit/events': {
      get: {
        tags: ['Audit'],
        summary: 'List audit events (TypeScript SDK)',
        parameters: [
          { name: 'page', in: 'query', schema: { type: 'integer', minimum: 1 } },
          { name: 'per_page', in: 'query', schema: { type: 'integer', minimum: 1, maximum: 100 } },
          { name: 'action', in: 'query', schema: { type: 'string' } },
          { name: 'target_type', in: 'query', schema: { type: 'string' } },
          { name: 'target_id', in: 'query', schema: { type: 'string' } },
          { name: 'outcome', in: 'query', schema: { type: 'string', enum: ['success', 'client_error', 'server_error'] } },
          { name: 'from', in: 'query', schema: { type: 'string', format: 'date-time' } },
          { name: 'to', in: 'query', schema: { type: 'string', format: 'date-time' } },
        ],
        responses: { 200: { description: 'Paginated audit events' } },
      },
    },
  };
  return spec;
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
