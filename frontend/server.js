const http = require("node:http");
const https = require("node:https");
const fs = require("node:fs");
const path = require("node:path");

const port = 5173;
const backendTarget = new URL("http://127.0.0.1:8080");
const proxyClient = backendTarget.protocol === "https:" ? https : http;
const distDir = path.join(__dirname, "dist");
const rootDir = fs.existsSync(distDir) ? distDir : __dirname;

const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml; charset=utf-8"
};

function sendFile(response, filePath) {
  const ext = path.extname(filePath).toLowerCase();
  const stream = fs.createReadStream(filePath);

  response.writeHead(200, {
    "Content-Type": mimeTypes[ext] || "application/octet-stream"
  });

  stream.pipe(response);
  stream.on("error", () => {
    response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Server error");
  });
}

function proxyApiRequest(request, response) {
  const proxyRequest = proxyClient.request(
    {
      protocol: backendTarget.protocol,
      hostname: backendTarget.hostname,
      port: backendTarget.port,
      method: request.method,
      path: request.url,
      headers: {
        ...request.headers,
        host: backendTarget.host,
        "x-forwarded-for": request.socket.remoteAddress || "",
        "x-forwarded-host": request.headers.host || "",
        "x-forwarded-proto": "http"
      }
    },
    (proxyResponse) => {
      response.writeHead(proxyResponse.statusCode || 502, proxyResponse.headers);
      proxyResponse.pipe(response);
    }
  );

  proxyRequest.on("error", () => {
    response.writeHead(502, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ message: "Backend service is unavailable" }));
  });

  request.pipe(proxyRequest);
}

const server = http.createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);

  if (url.pathname === "/api" || url.pathname.startsWith("/api/")) {
    proxyApiRequest(request, response);
    return;
  }

  const requestedPath = decodeURIComponent(url.pathname === "/" ? "/index.html" : url.pathname);
  const filePath = path.normalize(path.join(rootDir, requestedPath));

  const relativePath = path.relative(rootDir, filePath);

  if (relativePath.startsWith("..") || path.isAbsolute(relativePath)) {
    response.writeHead(403, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Forbidden");
    return;
  }

  fs.stat(filePath, (error, stats) => {
    if (error || !stats.isFile()) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not found");
      return;
    }

    sendFile(response, filePath);
  });
});

server.listen(port, () => {
  console.log(`Intern manager is running at http://localhost:${port}`);
  console.log(`Proxying /api to ${backendTarget.origin}`);
});
