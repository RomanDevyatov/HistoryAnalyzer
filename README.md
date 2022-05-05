Анализатор истории
==================
* * *

# Описание

Данная программа создает отчёт в формате .xls. Отчет представляет собой таблицу, которая хранит следующие данные: 
* имя пользователя (по горизонтали)
* дата (по вертикали)
* в ячейках - количество ссылок содержащих заданную строку, например, joker.

Данная программа работает в паре с [HistoryCopyMaker](https://github.com/RomanDevyatov/HistoryCopyMaker), которая создает текстовые файлы в папке ResultHistory.

Таким образом есть две программы:
* Анализатор
* [Копирователь](https://github.com/RomanDevyatov/HistoryCopyMaker)

P.S.: В общем и целом Вы можете просто создать папку, и в ней создать текстовые файлы с определенной ниже структурой. 

# Требования
Для запуска необходима установленная jre 8 (java environment). Описание подробной установки можно найти в README.md [здесь](https://github.com/RomanDevyatov/HistoryCopyMaker).

# Описание текстового файла истории

Название файла представляет собой строку: 
> username_historyRes_YYYY-MM-dd.txt

Строки файла истории имеет следующую структуру: 
> url, Visited On YYYY-MM-dd HH:mm:ss

# Запуск (проверено на Windows, на Linux и Macos тоже должно работать)

1)	Файл HistoryAnalyzer-1.0-SNAPSHOT.jar помещаем в C:\\\\Users\\\\Public\\\\<result_history_folder>\\\\<new_folder>,
     `<result_history_folder>` - папка, в которой находится папка, в которой хранятся файлы истории
     `<new_folder>` - просто созданная папка
      > P.S.: Необязательно, чтобы <new_folder> была в <result_history_folder>. 
2)  В <new_folder> создаем bat файл `startAnalyzer.bat` - файл для запуска анализатора, который содержит следующую строку:
      start java -jar HistoryAnalyzer-1.0-SNAPSHOT.jar <path_to>/<result_history_folder> <search_string> <log_on>
    * `<path_to>/<result_history_folder>` - путь до папки, где хранятся файлы истории
    * `<search_string>` - строка, по которой вести подсчёт, если не указывать этот параметр, то по умолчанию будет значение contactsOpened=true
    * `<log_on>` - если на месте этого аргумента указать log_on, то будет производиться логирование в файл по пути <path_to>/<result_history_folder>/log/Analyzer
                 если ничего не указывать или указать другую строку отличную от log_on, то логирования происходить не будет
    ### Пример:
    ```cmd
    start java -jar HistoryAnalyzer-1.0-SNAPSHOT.jar C:/Users/public/TestHistory contactsOpened=true log_on
    ```
3)  После указания всех параметров, запускаем `startAnalyzer.bat` и в <path_to>/<result_history_folder>/Otchet появляется .xls файл: MOSGeneralOtchet-YYYY-MM-dd_HH_mm_ss.xls
