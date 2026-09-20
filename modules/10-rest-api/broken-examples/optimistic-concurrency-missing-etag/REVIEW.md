# Code Review: Optimistic Concurrency & Missing ETag

## Context
A collaborative document editing service allows users to read and update articles. Multiple editors may fetch the same document version and submit updates simultaneously.

## Target Files
- [`ArticleDocument.java`](ArticleDocument.java)
- [`DocumentEditorController.java`](DocumentEditorController.java)

## Task
Review `DocumentEditorController.java`. Identify race condition vulnerabilities, lost updates, and missing HTTP conditional headers (`ETag`, `If-Match`).
