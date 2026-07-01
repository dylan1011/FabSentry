#!/bin/bash
lsof -ti:5555,5556,5557,5558 | xargs kill 2>/dev/null
echo "All tools stopped."
