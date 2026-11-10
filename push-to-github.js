const git = require('isomorphic-git');
const http = require('isomorphic-git/http/node');
const fs = require('fs');

const token = process.argv[2] || process.env.GITHUB_TOKEN || process.env.GH_TOKEN;

if (!token) {
  console.error('Error: Please provide your GitHub Personal Access Token (PAT).');
  console.error('Usage: node push-to-github.js <YOUR_GITHUB_TOKEN>');
  console.error('Generate a token at: https://github.com/settings/tokens (select "repo" scope)');
  process.exit(1);
}

(async () => {
  console.log('Pushing main branch to https://github.com/kirtanaldeshmukh-jpg/Public_Grv.git ...');
  const pushResult = await git.push({
    fs,
    http,
    dir: '.',
    remote: 'origin',
    ref: 'main',
    onAuth: () => ({ username: token }),
  });
  console.log('Push completed successfully! All code is live on GitHub.');
  console.log(pushResult);
})().catch(err => {
  console.error('Push failed:', err.message);
  process.exit(1);
});

