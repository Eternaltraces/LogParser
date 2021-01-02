# LogParser

This is a task for parsing log in provided format lined JSON.

Program does the following:

  - Take the path to the file as the running argument
  - Parse the lJSON file line by line and put it into internal memory
  - Sort the data to join FINISHED and STARTED events into single line with their respective start and end date, calculate the runtime and determine if it's in alert zone or not
  - Create new database file, table and populate it with all the entries, specifying which ones have the flag "alert" triggered.
  
  To run program  run "java -jar LogParser-1.0.jar log.txt" inside the /LogParser/build/libs folder. If your log file is in different location use the full location instead of log.txt file. Otherwise erase the preexisting jar file and build it from gradle by using gradle clean jar command on the main LogParser folder. 
