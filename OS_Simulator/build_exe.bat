@echo off
cd /d "%~dp0"
pip install pyinstaller
pyinstaller --noconfirm --onefile --windowed --name OS_Simulator main.py
echo.
echo Done. The program is in dist\OS_Simulator.exe
pause
