const { execSync } = require('child_process');
const packageJson = require('../package.json');

const branchName = process.argv[2];
if (!branchName) {
  console.error('Branch name is required');
  process.exit(1);
}

const sanitizedBranch = branchName.replace(/[^a-zA-Z0-9]/g, '-').replace(/^-+|-+$/g, '');
const baseVersion = packageJson.version;
const versionPrefix = `${baseVersion}-${sanitizedBranch}`;

console.error(`Prefix: ${versionPrefix}`);

let versions = [];
try {
  const stdout = execSync(`npm view ${packageJson.name} versions --json`, { stdio: ['pipe', 'pipe', 'ignore'] }).toString();
  versions = JSON.parse(stdout);
  if (!Array.isArray(versions)) {
    versions = [versions];
  }
} catch (e) {
  console.error('Package not found or no versions. Starting from 1.');
}

let maxIncrement = 0;

versions.forEach(v => {
  if (v.startsWith(versionPrefix)) {
    const suffix = v.slice(versionPrefix.length);
    if (suffix.startsWith('.')) {
      const numStr = suffix.substring(1);
      const num = parseInt(numStr, 10);
      if (!isNaN(num) && num > maxIncrement) {
        maxIncrement = num;
      }
    }
  }
});

const nextVersion = `${versionPrefix}.${maxIncrement + 1}`;
console.log(nextVersion);
