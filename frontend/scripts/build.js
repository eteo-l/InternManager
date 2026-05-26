const fs = require("node:fs");
const path = require("node:path");

const projectRoot = path.join(__dirname, "..");
const distDir = path.join(projectRoot, "dist");

function copyRecursive(source, target) {
  const stats = fs.statSync(source);

  if (stats.isDirectory()) {
    fs.mkdirSync(target, { recursive: true });
    fs.readdirSync(source).forEach((entry) => {
      copyRecursive(path.join(source, entry), path.join(target, entry));
    });
    return;
  }

  fs.copyFileSync(source, target);
}

fs.rmSync(distDir, { recursive: true, force: true });
fs.mkdirSync(distDir, { recursive: true });
copyRecursive(path.join(projectRoot, "index.html"), path.join(distDir, "index.html"));
copyRecursive(path.join(projectRoot, "intern.html"), path.join(distDir, "intern.html"));
copyRecursive(path.join(projectRoot, "mentor.html"), path.join(distDir, "mentor.html"));
copyRecursive(path.join(projectRoot, "favicon.svg"), path.join(distDir, "favicon.svg"));
copyRecursive(path.join(projectRoot, "app.config.js"), path.join(distDir, "app.config.js"));
copyRecursive(path.join(projectRoot, "src"), path.join(distDir, "src"));

console.log(`Build complete: ${distDir}`);
