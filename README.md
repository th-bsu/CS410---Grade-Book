# CS410-PROJECT:

Collaborators: Stark, A. & Ho T.

Boise State University

Spring 2020

# PROJECT DESCRIPTION:

designs and implements a JAVA application to manage grades in a class, using Cliche' as a command shell application.

# SETUP, CONNECT, COMPILE, AND EXECUTE:

   Database (i.e. Onyx head node):

   Setup: "Setting up PostgreSQL" PDF.

   Connect: psql -U 'user-name' -p 3XXXX -h /tmp 'database-name'

   Cliche Compile:

   clear && clear

   cd top-level/asg.cliche/src/asg/cliche/util

   javac *.java

   cd top-level/asg.cliche/src/asg/cliche

   javac -cp .:../.. *.java

   GradeBookShell Compile:

   cd top-level/src/main/java

   clear && clear

   javac -cp .:../../../lib/postgresql-42.2.12.jar:../../../asg.cliche/src edu/boisestate/cs410/gradebook/GradeBookShell.java

   GradeBookShell Execute:

   java  -cp .:../../../lib/postgresql-42.2.12.jar:../../../asg.cliche/src edu.boisestate.cs410.gradebook.GradeBookShell 'postgresql://localhost:3XXXX/database-name?user=user-name&password=your-password-here'

   Shell Script Execute:

   cd top-level/

   chmod +x GradeBookShell.sh && ./GradeBookShell.sh

   Maven Setup, Compile, and Run (i.e. AFTER Cliche Compile Complete AND pom.xml updated):

   cd top-level/asg.cliche/src/asg/cliche

   cp *.class top-level/target/classes/asg/cliche/.

   cd top-level/asg.cliche/src/asg/cliche/util

   cp *.class top-level/target/classes/asg/cliche/util/.

   cd top-level/

   mvn package

   java -jar target/gradebook-1-jar-with-dependencies.jar 'postgresql://localhost:3XXXX/database-name?user=user-name&password=your-password-here'

   ;

# SAMPLE DATABASE QUERIES:

   database-cs410=# \dt                                                                             -> lists all relations.

   database-cs410=# select * from class_   order by id_ desc limit 10;                              -> retrieves all classes  in database.

   database-cs410=# select * from student_ order by id_ desc limit 10;                              -> retrieves all students in database.


   database-cs410=# SELECT id_, course_, term_, section_, COUNT(id_student_) AS student_count       -> retrieves all classes and number of students enrolled in each class.

                    FROM class_

                    JOIN enrollment_ ON class_.id_ = enrollment_.id_class_

                    GROUP BY class_.id_, class_.term_

                    ORDER BY student_count

                    ;

   database-cs410=# SELECT student_.id_, student_.username_, student_.realname_, enrollment_.grade_ -> retrieves student information in given class.

                    FROM class_

                    JOIN enrollment_ ON class_.id_ = enrollment_.id_class_

                    JOIN student_ ON enrollment_.id_student_ = student_.id_

                    WHERE class_.id_ = 900

                    GROUP BY student_.id_, student_.username_, enrollment_.grade_

                    ORDER BY student_.username_ DESC

                    LIMIT 200

                    ;

   database-cs410=# select * from item_grade_ where id_student_ = 376 and id_item_ = 600 ;          -> retrieves item grade for compatible (student,item) combo.

   database-cs410=# select * from enrollment_ where id_class_ = 330 ;                               -> lists all students enrolled in given class.

   database-cs410=# select * from enrollment_ where id_student_ = 376;                              -> lists all classes enrolled by given student.

   database-cs410=# select * from item_grade_ where id_student_ = 2672 ;                            -> retrieves all item grades from all classes enrolled by given student.

   ;

# SAMPLE SHELL COMMANDS:

   gradebook> ?list                                                  -> lists all available functions to be invoked.

   gradebook> list-classes                                           -> lists all classes (with at least ONE student enrolled) in the gradebook.

   ;

   gradebook> new-class CS410 Fall2021 11 'Final Section'            -> adds new class to database.

   gradebook> new-class CS410 Fall2021 12 'Final Section Again'      -> adds new class to database.

   gradebook> select-class CS410                                     -> fails due to multiple (unique) sections.

   gradebook> select-class CS410 Fall2021                            -> fails due to multiple (unique) sections.

   gradebook> select-class CS410 Fall2021 11                         -> selects particular class for debug.

   ;

   gradebook> select-class CS110 Fall2019 03                         -> selects particular class for debug.

   gradebook> show-class                                             -> shows selected class.

   gradebook> add-category HOMEWORK 0.4                              -> adds given (unique) category to selected class (i.e. gets executed ONLY once, otherwise duplicate error).

   gradebook> show-categories                                        -> shows all categories in selected class.

   gradebook> add-item '100' 'HOMEWORK' 'Final Item.' 1000.5         -> adds item to database.

   gradebook> show-items                                             -> shows all items in selected class.

   ;

   gradebook> select-class CS110 Fall2021 10                         -> selects particular class for debug.

   gradebook> add-student 'FinalStudent' 5001 'Student, Final'       -> adds student [to database and] to selected class (immediately above).

   gradebook> add-student 'FinalStudent' 5001 'Final-Student'        -> changes student's realname.

   gradebook> add-student 'FinalStudentUpdated' 5001 'Final-Student' -> changes student's username.

   gradebook> add-student 'FinalStudent' 5001 'Student, Final'       -> changes student's realname and username OR tests for 'Already Enrolled, NO Name Mismatched'.

   gradebook> add-student 'name-unknown'                             -> tests for unknown username.

   gradebook> show-students                                          -> shows all students in given class.

   ;

   gradebook> select-class CS110 Fall2021 10                         -> selects particular class for debug.

   gradebook> add-student 'FinalStudent'                             -> tests for 'Given Student Already Enrolled In Selected Class'.

   gradebook> show-students                                          -> shows all students in given class.

   gradebook> show-students 'FiN'                                    -> searches for any student(s) with given pattern (i.e. case-insensitive).

   gradebook> show-students 'unknown'                                -> tests for unknown string match.

   ;

   gradebook> select-class CS104 Fall2019 10                         -> selects particular class for debug.

   gradebook> grade 'PROJECT-10' 'vbissekeraf' 20                    -> assigns item grade for compatible (student,item) combo (i.e. 1st-time: insert).

   gradebook> grade 'PROJECT-10' 'vbissekeraf' 30.5                  -> assigns item grade for compatible (student,item) combo (i.e. 2nd-time: update).

   gradebook> grade 'PROJECT-10' 'vbissekeraf' 500.5                 -> tests for warning (i.e. maximum configured point exceeded).

   gradebook> grade 'HOMEWORK-5' 'hwibrowin' 20                      -> tests for student-item compatibility (or specifically, lack of it).

   gradebook> student-grades 'hwibrowin'                             -> shows current grade(s) for given student in given class, grouped by category.

   gradebook> student-grades 'vbissekeraf'                           -> shows current grade(s) for given student in given class, grouped by category.

   ;

   gradebook> select-class CS110 Fall2021 10                         -> selects particular class for debug.

   gradebook> student-grades 'vbissekeraf'                           -> tests for 'No Enrollment Record Found'.

   gradebook> student-grades 'hwibrowin'                             -> tests for 'No Enrollment Record Found'.

   gradebook> student-grades 'FinalStudent'                          -> shows current grade(s) for given student in given class, grouped by category.

   ;

   gradebook> select-class CS110 Fall2021 10                         -> selects particular class for debug.

   gradebook> gradebook                                              -> shows grade book for given class with default grade assigned (if applicable).

   ;

# SAMPLE TEST SEQUENCE FOR GRADE IMPORTS FOR GIVEN ITEM IN GIVEN CLASS:

   gradebook> select-class CS110 Fall2021 10                         -> selects particular class for debug.

   gradebook> ig 'MIDTERM-10' 'gradefile.csv'                        -> assigns grade to each student (i.e. in given class) on given valid item.

   gradebook> ig 'MIDTERM-2'  'gradefile_0.csv'                      -> assigns grade to each student (i.e. in given class) on given valid item.

   gradebook> ig 'PROJECT-7'  'gradefile_1.csv'                      -> assigns grade to each student (i.e. in given class) on given valid item.

   gradebook> ig 'PROJECT-77' 'gradefile_1.csv'                      -> assigns grade to each student (i.e. in given class) on given IN-valid item.

   ;

# SAMPLE TEST SEQUENCE FOR GRADE CALCULATION FOR GIVEN STUDENT:

   Select given class for debug:

      gradebook> select-class CS104 Fall2019 10

   Initially, 'vbissekeraf' may NOT have all item grades entered into database -> official grade NOT yet available.

      gradebook> student-grades 'vbissekeraf'

   Sequentially enter (or update) item grades for 'vbissekeraf' on console.

      gradebook> grade 'PROJECT-10' 'vbissekeraf' 500

      gradebook> grade 'FINAL-9'    'vbissekeraf' 250

      gradebook> grade 'FINAL-2'    'vbissekeraf' 500

      gradebook> grade 'MIDTERM-4'  'vbissekeraf' 305

      gradebook> grade 'HOMEWORK-4' 'vbissekeraf' 200

   Finally, 'vbissekeraf' should have all (new or updated) item grades from database -> official grade NOW available.

      gradebook> student-grades 'vbissekeraf'

   ;

# DEBUG INFO 0:

   student_.id_ = 376  -> username: vbissekeraf

   class_.id_ = 330    -> CS104 Fall2019 10

   item_.id_ = 600     -> name: PROJECT-10

   category_.id_ = 155 -> name: PROJECT

   ;

# DEBUG INFO 1:

   student_.id_ = 2672 -> username: hwibrowin

   class_.id_ = 330    -> CS104 Fall2019 10

   ;

# DEBUG INFO 2:

   student_.id_ = 494  -> username: kedwickerdp

   class_.id_ = 330    -> CS104 Fall2019 10

   ;

# DEBUG INFO 3:

   class_.id_ = 863    -> CS110 Fall2019 03

   class_.id_ = 900    -> CS110 Fall2021 10

   ;

;
