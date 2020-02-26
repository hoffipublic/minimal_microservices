#!/usr/bin/env bash

find . -type l -exec ls -Ggh {} + | awk '{print $8 $9 $10}'

