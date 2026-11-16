# System Capabilities — offline planning example

Use Python 3 standard library with an explicit target platform and requested capability. No GPS, device, network, camera, OS permission, process or unique identifier is accessed.

~~~bash
python scripts/system_capability_check.py --platform windows --capability location --capability time
python scripts/system_capability_check.py --platform browser --capability camera --capability files
python scripts/system_capability_check.py --platform unknown --capability bluetooth
python -m unittest discover -s tests -p 'test_system_capabilities_permissions.py' -v
~~~

CANDIDATE only identifies a documentation-backed possibility; UNASSESSED means no platform candidate; NOT_REQUESTED means no host consent; NOT RUN is native testing status. [Skill](../../skills/system-capabilities-and-permissions/SKILL.md) · [guide](../../references/system-capabilities-permissions.md).
