#!/bin/bash
set -e

# Определяем текущую ветку
BRANCH=${GITHUB_HEAD_REF:-${GITHUB_REF_NAME}}
echo "Current branch: $BRANCH"

# Обновляем origin/main для корректного diff
git fetch origin main

# Выбираем базовый коммит для diff
if [ "$BRANCH" = "main" ]; then
    BASE=$(git rev-parse HEAD^)
else
    BASE=$(git merge-base origin/$BRANCH origin/main)
fi
echo "Base commit for diff: $BASE"

# Все изменённые, добавленные, переименованные и скопированные файлы любых типов
ALL_CHANGED_FILES=$(git diff --diff-filter=ACMRT --name-only $BASE HEAD | tr '\n' ';')
echo "All changed/added/renamed/copied files: $ALL_CHANGED_FILES"

# Все удалённые файлы любых типов
ALL_DELETED_FILES=$(git diff --diff-filter=D --name-only $BASE HEAD | tr '\n' ';')
echo "All deleted files: $ALL_DELETED_FILES"

FULL_RUN=false

# Любой удалённый файл, кроме тестовых .java → FULL_RUN
if [ -n "$ALL_DELETED_FILES" ] && echo "$ALL_DELETED_FILES" | grep -q -v "Test\.java"; then
  FULL_RUN=true
  echo "Non-test deleted file detected → FULL_RUN"
fi

# Любой изменённый/добавленный/переименованный/скопированный файл, кроме тестовых .java → FULL_RUN
if [ -n "$ALL_CHANGED_FILES" ] && echo "$ALL_CHANGED_FILES" | grep -q -v "Test\.java"; then
  FULL_RUN=true
  echo "Non-test changed/added/renamed/copied file detected → FULL_RUN"
fi

# Только тестовые .java файлы для FilterForTests
CHANGED_TEST_FILES=$(echo "$ALL_CHANGED_FILES" | grep '\.java$' | tr '\n' ';')
echo "Test .java files to run: $CHANGED_TEST_FILES"

# Устанавливаем переменную для GitHub Actions
if [ "$FULL_RUN" = true ]; then
  echo "LIST_OF_CHANGED_FILES=FULL_RUN"
  echo "LIST_OF_CHANGED_FILES=FULL_RUN" >> $GITHUB_ENV
else
  echo "LIST_OF_CHANGED_FILES=$CHANGED_TEST_FILES"
  echo "LIST_OF_CHANGED_FILES=$CHANGED_TEST_FILES" >> $GITHUB_ENV
fi
