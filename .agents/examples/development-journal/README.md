# Offline development journal example (not native GIS execution)

These commands require an existing disposable project directory `demo-app` and must be run from a checkout of GIS Development Skills. They create **journal reports only**, not an actual GIS tool.

~~~bash
mkdir demo-app
python scripts/development_journal.py init --project ./demo-app
python scripts/development_journal.py add --project ./demo-app --kind request --status proposed --summary "طلب تعديل ملفات KML" --details "احفظ الأيقونات والألوان الأصلية" --task-id KML-1 --plan-revision r1
python scripts/development_journal.py add --project ./demo-app --kind revision --status proposed --summary "غيّر الرموز" --details "تعديل جديد للطلب الأول" --ref 1
python scripts/development_journal.py add --project ./demo-app --kind cancellation --status cancelled --summary "إلغاء التعديل" --details "تم إلغاء الطلب الثاني" --ref 2
python scripts/development_journal.py verify --project ./demo-app
python scripts/development_journal.py status --project ./demo-app
python -m unittest discover -s tests -p 'test_development_journal.py' -v
~~~

Inspect `.agent-development-log/records/0001.json`, `reports/0001.md`, `INDEX.md` and `STATE.md` in the **target** project. Reports `0002` and `0003` link previous step numbers without modifying `0001`. No files were deleted, and no source code change, native app test or actual rollback is claimed.

If a generated report is missing but canonical record checks pass, run `python scripts/development_journal.py repair --project ./demo-app`. It never repairs corrupted canonical source records. Store only privacy-safe summaries.