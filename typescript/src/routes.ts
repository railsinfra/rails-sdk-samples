import type { Application, Request, Response, NextFunction } from 'express';
import { randomUUID } from 'crypto';
import Rails from '@railsinfra/rails-typescript';
import type { RailsXEnvironment } from './config';
import { HttpError } from './errors';

type RailsClient = InstanceType<typeof Rails>;

export interface RouteDeps {
  baseURL: string;
  apiKey: string;
  client: RailsClient;
  proxyFetch: typeof fetch;
}

function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<void>,
): (req: Request, res: Response, next: NextFunction) => void {
  return (req, res, next) => {
    void fn(req, res, next).catch(next);
  };
}

function genIdempotencyKey(prefix: string): string {
  return `${prefix}-${Date.now()}-${randomUUID().replace(/-/g, '')}`;
}

function trimBase(u: string): string {
  return u.replace(/\/$/, '');
}

const SANDBOX_ENV: RailsXEnvironment = 'sandbox';

function sdkEnvHeaders(): { 'X-Environment': RailsXEnvironment } {
  return { 'X-Environment': SANDBOX_ENV };
}

async function forwardResponse(
  res: Response,
  proxyFetch: typeof fetch,
  url: string,
  init: RequestInit,
): Promise<void> {
  const upstream = await proxyFetch(url, init);
  const text = await upstream.text();
  res.status(upstream.status).type('application/json').send(text);
}

export function registerRoutes(app: Application, deps: RouteDeps): void {
  const { baseURL, apiKey, client, proxyFetch } = deps;
  const root = trimBase(baseURL);

  const postCreate = asyncHandler(async (req: Request, res: Response) => {
    await forwardResponse(res, proxyFetch, `${root}/api/v1/accounts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': apiKey,
        'X-Environment': SANDBOX_ENV,
      },
      body: JSON.stringify(req.body ?? {}),
    });
  });

  const getListAccounts = asyncHandler(async (req: Request, res: Response) => {
    const qs = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
    await forwardResponse(res, proxyFetch, `${root}/api/v1/accounts${qs}`, {
      method: 'GET',
      headers: {
        'X-API-Key': apiKey,
        'X-Environment': SANDBOX_ENV,
      },
    });
  });

  app.post('/api/accounts', postCreate);
  app.post('/api/v1/accounts', postCreate);
  app.get('/api/accounts', getListAccounts);
  app.get('/api/v1/accounts', getListAccounts);

  const postDeposit = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const body = JSON.stringify(req.body ?? {});
    if (body === '{}' || body === 'null') throw new HttpError(400, 'missing body');
    const idempotencyKey = (req.headers['idempotency-key'] as string) || genIdempotencyKey('dep');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
      'Idempotency-Key': idempotencyKey,
      'X-Environment': SANDBOX_ENV,
    };
    await forwardResponse(res, proxyFetch, `${root}/api/v1/accounts/${id}/deposit`, {
      method: 'POST',
      headers,
      body,
    });
  });

  const postTransfer = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const body = JSON.stringify(req.body ?? {});
    if (body === '{}' || body === 'null') throw new HttpError(400, 'missing body');
    const idempotencyKey = (req.headers['idempotency-key'] as string) || genIdempotencyKey('xfr');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
      'Idempotency-Key': idempotencyKey,
      'X-Environment': SANDBOX_ENV,
    };
    await forwardResponse(res, proxyFetch, `${root}/api/v1/accounts/${id}/transfer`, {
      method: 'POST',
      headers,
      body,
    });
  });

  const postWithdraw = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const body = JSON.stringify(req.body ?? {});
    if (body === '{}' || body === 'null') throw new HttpError(400, 'missing body');
    const idempotencyKey = (req.headers['idempotency-key'] as string) || genIdempotencyKey('wdr');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
      'Idempotency-Key': idempotencyKey,
      'X-Environment': SANDBOX_ENV,
    };
    await forwardResponse(res, proxyFetch, `${root}/api/v1/accounts/${id}/withdraw`, {
      method: 'POST',
      headers,
      body,
    });
  });

  app.post('/api/accounts/:id/deposit', postDeposit);
  app.post('/api/v1/accounts/:id/deposit', postDeposit);
  app.post('/api/accounts/:id/transfer', postTransfer);
  app.post('/api/v1/accounts/:id/transfer', postTransfer);
  app.post('/api/accounts/:id/withdraw', postWithdraw);
  app.post('/api/v1/accounts/:id/withdraw', postWithdraw);

  const getAccount = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const data = await client.accounts.retrieve(id, { headers: sdkEnvHeaders() });
    res.json(data);
  });

  app.get('/api/accounts/:id', getAccount);
  app.get('/api/v1/accounts/:id', getAccount);

  const deleteAccount = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const data = await client.accounts.close(id, { headers: sdkEnvHeaders() });
    res.json(data);
  });

  app.delete('/api/accounts/:id', deleteAccount);
  app.delete('/api/v1/accounts/:id', deleteAccount);

  const patchStatus = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const body = req.body as { status?: string };
    if (!body?.status) throw new HttpError(400, 'status required');
    const status = body.status as 'active' | 'suspended' | 'closed';
    const data = await client.accounts.updateStatus(id, { status }, { headers: sdkEnvHeaders() });
    res.json(data);
  });

  app.patch('/api/accounts/:id/status', patchStatus);
  app.patch('/api/v1/accounts/:id/status', patchStatus);
  app.patch('/api/accounts/:id', patchStatus);
  app.patch('/api/v1/accounts/:id', patchStatus);

  const rawGet = asyncHandler(async (req: Request, res: Response) => {
    const pathParam = (req.query.path as string) || 'api/v1/accounts';
    const url = `${root}/${pathParam.replace(/^\//, '')}`;
    await forwardResponse(res, proxyFetch, url, {
      method: 'GET',
      headers: { 'X-API-Key': apiKey },
    });
  });

  const rawPost = asyncHandler(async (req: Request, res: Response) => {
    const pathParam = (req.query.path as string) || 'api/v1/accounts';
    const url = `${root}/${pathParam.replace(/^\//, '')}`;
    await forwardResponse(res, proxyFetch, url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': apiKey,
      },
      body: typeof req.body === 'string' ? req.body : JSON.stringify(req.body ?? {}),
    });
  });

  app.get('/api/raw/get', rawGet);
  app.get('/api/v1/raw/get', rawGet);
  app.post('/api/raw/post', rawPost);
  app.post('/api/v1/raw/post', rawPost);

  const getTx = asyncHandler(async (req: Request, res: Response) => {
    const id = req.params.id;
    if (!id) throw new HttpError(400, 'missing id');
    const data = await client.transactions.retrieve(id, { headers: sdkEnvHeaders() });
    res.json(data);
  });

  app.get('/api/transactions/:id', getTx);
  app.get('/api/v1/transactions/:id', getTx);

  const listByAccount = asyncHandler(async (req: Request, res: Response) => {
    const accountId = req.params.accountId;
    if (!accountId) throw new HttpError(400, 'missing accountId');
    const limitRaw = req.query.limit;
    const limit =
      typeof limitRaw === 'string' ? Number.parseInt(limitRaw, 10) : Array.isArray(limitRaw) ? Number.parseInt(String(limitRaw[0]), 10) : undefined;
    const data = await client.transactions.listByAccount(
      accountId,
      limit !== undefined && Number.isFinite(limit) ? { limit } : {},
      { headers: sdkEnvHeaders() },
    );
    res.json(data);
  });

  app.get('/api/accounts/:accountId/transactions', listByAccount);
  app.get('/api/v1/accounts/:accountId/transactions', listByAccount);
}
