#!/bin/bash
set -e

# Определяем текущую ветку (PR → GITHUB_HEAD_REF, обычный пуш → GITHUB_REF_NAME)
BRANCH=${GITHUB_HEAD_REF:-${GITHUB_REF_NAME}}
echo "Current branch: $BRANCH"

# Находим общий предок ветки с main
BASE=$(git merge-base origin/$BRANCH origin/main)
echo "Common ancestor (BASE): $BASE"

# Все добавленные, изменённые, переименованные и скопированные .java файлы
CHANGED_FILES=$(git diff --diff-filter=ACMRT --name-only $BASE origin/$BRANCH | grep '\.java$' | tr '\n' ';')
echo "Changed/added/renamed/copied .java files: $CHANGED_FILES"

# Все удалённые .java файлы
DELETED_FILES=$(git diff --diff-filter=D --name-only $BASE origin/$BRANCH | grep '\.java$' | tr '\n' ';')
echo "Deleted .java files: $DELETED_FILES"

FULL_RUN=false

# Проверяем удалённые файлы на не тестовые
if echo "$DELETED_FILES" | grep -q -v "Test\.java"; then
  FULL_RUN=true
  echo "Non-test deleted file detected → FULL_RUN"
fi

# Проверяем добавленные/изменённые/переименованные/скопированные файлы на не тестовые
if echo "$CHANGED_FILES" | grep -q -v "Test\.java"; then
  FULL_RUN=true
  echo "Non-test changed/added/renamed/copied file detected → FULL_RUN"
fi

# Устанавливаем переменную для GitHub Actions
if [ "$FULL_RUN" = true ]; then
  echo "LIST_OF_CHANGED_FILES=FULL_RUN"
  echo "LIST_OF_CHANGED_FILES=FULL_RUN" >> $GITHUB_ENV
else
  echo "LIST_OF_CHANGED_FILES=$CHANGED_FILES"
  echo "LIST_OF_CHANGED_FILES=$CHANGED_FILES" >> $GITHUB_ENV
fi
