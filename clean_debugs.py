import os
import re

directory = r"c:\MinecraftModdingWorkspace\1.20.1 Mods\Pufferfish-Skill-Leveling-1.20.1_addon_version\Pufferfish-Skill-Leveling"

pattern1 = re.compile(r'^[ \t]*(?:[a-zA-Z0-9_]+\.getInstance\(\)\.)?getLogger\(\)\s*\.debug\([^;]+;\r?\n', re.MULTILINE)
pattern2 = re.compile(r'^[ \t]*[a-zA-Z0-9_]+\.getInstance\(\)\.getLogger\(\)\s*\r?\n\s*\.debug\([^;]+;\r?\n', re.MULTILINE)
pattern3 = re.compile(r'^[ \t]*logger\s*\.debug\([^;]+;\r?\n', re.MULTILINE)
pattern4 = re.compile(r'^[ \t]*System\.(?:out|err)\.println\(.*"DEBUG:.*[^;]+;\r?\n', re.MULTILINE)

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                orig = content
                
                content = pattern1.sub('', content)
                content = pattern2.sub('', content)
                content = pattern3.sub('', content)
                content = pattern4.sub('', content)
                
                if orig != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)
                    print(f'Cleaned {file}')
            except Exception as e:
                pass
