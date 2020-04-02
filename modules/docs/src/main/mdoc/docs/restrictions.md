---
layout: docs
title: Troubleshooting/Restrictions
---

## Troubleshooting/Restrictions

Methods containing `memoize` blocks must have an explicit return type.
If you don't specify the return type, you'll get a confusing compiler error along the lines of `recursive method withExpiry needs result type`.

For example, this is OK

```scala
def getUser(id: Int): Future[User] = memoize {
  // Do stuff...
}
```

but this is not

```scala
def getUser(id: Int) = memoize {
  // Do stuff...
}
```
