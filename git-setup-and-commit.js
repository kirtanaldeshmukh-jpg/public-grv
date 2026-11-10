const git = require('isomorphic-git');
const http = require('isomorphic-git/http/node');
const fs = require('fs');
const path = require('path');

const dir = process.cwd();

async function main() {
  console.log('Initializing Git repository in', dir);
  await git.init({ fs, dir, defaultBranch: 'main' });

  console.log('Adding files to Git index...');
  
  // Recursively add all files respecting .gitignore
  const files = await git.listFiles({ fs, dir });
  console.log('Existing tracked files:', files.length);

  // Status matrix to find all untracked files
  const statusMatrix = await git.statusMatrix({ fs, dir });
  let addedCount = 0;
  for (const [filepath, head, workdir, stage] of statusMatrix) {
    if (workdir !== 0) { // exists in workdir
      await git.add({ fs, dir, filepath });
      addedCount++;
    }
  }
  console.log(`Staged ${addedCount} files.`);

  console.log('Creating initial commit...');
  const sha = await git.commit({
    fs,
    dir,
    author: {
      name: 'Kirtana Deshmukh',
      email: 'kirtanaldeshmukh@gmail.com',
    },
    message: 'Initial commit: ResolveTrack Grievance Redressal & Statutory Escalation Portal (Spring Boot + React)',
  });
  console.log('Commit created with SHA:', sha);

  console.log('Configuring remote origin: https://github.com/kirtanaldeshmukh-jpg/Public_Grv.git');
  await git.setConfig({
    fs,
    dir,
    path: 'remote.origin.url',
    value: 'https://github.com/kirtanaldeshmukh-jpg/Public_Grv.git'
  });
  await git.setConfig({
    fs,
    dir,
    path: 'remote.origin.fetch',
    value: '+refs/heads/*:refs/remotes/origin/*'
  });
  await git.setConfig({
    fs,
    dir,
    path: 'branch.main.remote',
    value: 'origin'
  });
  await git.setConfig({
    fs,
    dir,
    path: 'branch.main.merge',
    value: 'refs/heads/main'
  });

  console.log('Local Git setup complete! Branch: main, Remote: origin');
}

main().catch(console.error);

