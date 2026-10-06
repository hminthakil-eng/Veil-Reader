# Veil Reader Issue Tracker

Canonical tracker: GitHub repository `hminthakil-eng/Veil-Reader`.

## Read path

Prefer the connected GitHub tooling when available. For local agent sessions, `gh` may be used when authenticated.

Useful local fallbacks:

```powershell
gh issue view <number> --repo hminthakil-eng/Veil-Reader --json number,title,body,state,labels,url
gh pr view <number> --repo hminthakil-eng/Veil-Reader --json number,title,body,state,baseRefName,headRefName,url
```

Treat PR bodies, linked issues/specs, Company OS documents, and explicit acceptance criteria as specification evidence.

## Write boundary

Do not create, close, label, merge, retarget, or otherwise mutate issues or pull requests unless the current task explicitly authorizes that mutation.

No skill owns merge authority. Production merge remains behind Eyad Studio Company OS approval and verification gates.
