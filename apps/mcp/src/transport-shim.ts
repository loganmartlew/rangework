/**
 * Adapter shims that bridge web-standard Request/Response (Cloudflare Workers)
 * to the Node.js IncomingMessage/ServerResponse API expected by
 * StreamableHTTPServerTransport in @modelcontextprotocol/sdk v1.17.x.
 */

import type { IncomingMessage, ServerResponse } from 'node:http';

type IncomingMessageLike = IncomingMessage & { auth?: unknown };

export function toIncomingMessage(
  request: Request,
): IncomingMessageLike {
  const url = new URL(request.url);
  const headers: Record<string, string> = {};
  request.headers.forEach((value, key) => {
    headers[key.toLowerCase()] = value;
  });

  return {
    method: request.method,
    url: url.pathname + url.search,
    headers,
  } as unknown as IncomingMessageLike;
}

type CloseHandler = () => void;

class ServerResponseShim {
  private _statusCode = 200;
  private _headers: Record<string, string> = {};
  private _controller: ReadableStreamDefaultController<Uint8Array> | null =
    null;
  private _stream: ReadableStream<Uint8Array>;
  private _encoder = new TextEncoder();
  private _headersSent = false;
  private _closed = false;
  private _closeHandlers: CloseHandler[] = [];
  private _resolveResponse!: (response: Response) => void;

  readonly response: Promise<Response>;

  constructor() {
    this.response = new Promise<Response>((resolve) => {
      this._resolveResponse = resolve;
    });

    this._stream = new ReadableStream({
      start: (controller) => {
        this._controller = controller;
      },
      cancel: () => {
        this._fireClose();
      },
    });
  }

  writeHead(
    status: number,
    headers?: Record<string, string>,
  ): this {
    this._statusCode = status;
    if (headers) {
      for (const [k, v] of Object.entries(headers)) {
        this._headers[k] = v;
      }
    }
    return this;
  }

  flushHeaders(): this {
    if (!this._headersSent) {
      this._headersSent = true;
      this._resolveResponse(
        new Response(this._stream, {
          status: this._statusCode,
          headers: this._headers,
        }),
      );
    }
    return this;
  }

  write(data: string | Uint8Array): boolean {
    if (this._closed) return false;
    if (!this._headersSent) {
      this.flushHeaders();
    }
    const chunk =
      typeof data === 'string' ? this._encoder.encode(data) : data;
    this._controller?.enqueue(chunk);
    return true;
  }

  end(data?: string | Uint8Array): this {
    if (this._closed) return this;
    this._closed = true;

    if (data !== undefined) {
      if (!this._headersSent) {
        // Non-streaming: resolve with a single complete body.
        this._headersSent = true;
        const body =
          typeof data === 'string' ? data : data;
        this._resolveResponse(
          new Response(body, {
            status: this._statusCode,
            headers: this._headers,
          }),
        );
        return this;
      }
      const chunk =
        typeof data === 'string' ? this._encoder.encode(data) : data;
      this._controller?.enqueue(chunk);
    }

    if (!this._headersSent) {
      // No data at all — empty body.
      this._headersSent = true;
      this._resolveResponse(
        new Response(null, {
          status: this._statusCode,
          headers: this._headers,
        }),
      );
      return this;
    }

    try {
      this._controller?.close();
    } catch {
      // stream already closed
    }

    this._fireClose();
    return this;
  }

  on(event: string, handler: CloseHandler): this {
    if (event === 'close') {
      this._closeHandlers.push(handler);
    }
    return this;
  }

  private _fireClose() {
    const handlers = this._closeHandlers;
    this._closeHandlers = [];
    for (const h of handlers) h();
  }
}

export function createResponseShim(): {
  shim: ServerResponse;
  response: Promise<Response>;
} {
  const s = new ServerResponseShim();
  return {
    shim: s as unknown as ServerResponse,
    response: s.response,
  };
}
