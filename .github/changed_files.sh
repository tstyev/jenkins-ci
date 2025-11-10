#!/bin/bash
set -e

# Находим общий предок ветки с main
BASE=$(git merge-base origin/$GITHUB_HEAD_REF origin/main)

# Берём все добавленные, изменённые, переименованные или скопированные .java файлы
CHANGED_FILES=$(git diff --diff-filter=ACMRT --name-only $BASE origin/$GITHUB_HEAD_REF | grep '\.java$' | tr '\n' ';')

# Если среди них есть хотя бы один не тестовый файл → FULL_RUN
if echo "$CHANGED_FILES" | grep -q -v "Test\.java"; then
  echo "LIST_OF_CHANGED_FILES=FULL_RUN" >> $GITHUB_ENV
else
  # Только тестовые файлы → список этих файлов
  echo "LIST_OF_CHANGED_FILES=$CHANGED_FILES" >> $GITHUB_ENV
fi
