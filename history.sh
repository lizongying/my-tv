#!/bin/bash

in_changelog=false

while IFS= read -r line; do
    if [[ "$line" == "## "* ]]; then
        continue
    fi

    if [[ $in_changelog == false ]] && [[ "$line" == "### "* ]]; then
        in_changelog=true
        continue
    fi

    if [[ $in_changelog == true ]] && [[ "$line" == "### "* ]]; then
        break
    fi

    echo "$line"
done < HISTORY.md
