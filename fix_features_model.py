# The problem is `CapturedFeature` was reverted when I did `git checkout main && git merge ...` and it failed because main was gone. Wait, I was on feature branch. Why did it fail?
# Let's check `git log -n 1`
import os
os.system("git status")
