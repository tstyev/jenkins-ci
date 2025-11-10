#!/bin/bash
set -e

# Находим ветку (pull request или push)
BRANCH=${GITHUB_HEAD_REF:-${GITHUB_REF_NAME}}

# Находим общий предок с main
BASE=$(git merge-base origin/$BRANCH origin/main)

# Берём все добавленные, изменённые, переименованные или скопированные .java файлы
CHANGED_FILES=$(git diff --diff-filter=ACMRT --name-only $BASE origin/$BRANCH | grep '\.java$' | tr '\n' ';')

# Если среди них есть хотя бы один нетестовый файл → FULL_RUN
if echo "$CHANGED_FILES" | grep -q -v "Test\.java"; then
  echo "LIST_OF_CHANGED_FILES=FULL_RUN" >> $GITHUB_ENV
else
  # Только тестовые файлы → список этих файлов
  echo "LIST_OF_CHANGED_FILES=$CHANGED_FILES" >> $GITHUB_ENV
fi
