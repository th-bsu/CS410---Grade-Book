package edu.boisestate.cs410.gradebook;

//import com.budhash.cliche.Command;
//import com.budhash.cliche.Param;
//import com.budhash.cliche.ShellFactory;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.util.regex.PatternSyntaxException;

public class GradeBookShell {
    private final Connection db;
    private final String noClassString = "\n - No class is currently selected.";

    public GradeBookShell(Connection cxn) {
        db = cxn;
    }

    @Command(description="Adds a new class to the schedule.")
    public String newClass(
            @Param(name="course", description="e.g. CS101")
                String courseStr,
            @Param(name="term", description="e.g. Summer2012")
                    String termStr,
            @Param(name="section", description="e.g. 01")
                    String sectionStr,
            @Param(name="description", description="e.g. Intro To Computer Science")
                    String descStr) {
        String insertCategory = "INSERT INTO class_ (course_, term_, section_, description_) VALUES (?, ?, ?, ?);";
        int categoryId;
        try {
            db.setAutoCommit(false);
            PreparedStatement stmt = db.prepareStatement(insertCategory, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, courseStr);
            stmt.setString(2, termStr);
            stmt.setString(3, sectionStr);
            stmt.setString(4, descStr);
            stmt.executeUpdate();
            //
//          ResultSet rs = stmt.executeQuery();
            db.commit();
        } catch (SQLException | RuntimeException e) {
            try{
                db.rollback();
            } catch (Exception E){
                // rollback failed
                return "SQL ERROR: " + e.toString() + E.toString();
            }
            return "SQL ERROR: " + e.toString();
            //throw e;
        } finally {
            try{
                db.setAutoCommit(true);
            } catch (Exception e){
                // autocommit switch failed
                return "SQL ERROR: " + e.toString();
            }
        }
        return "Success";
    }

    @Command(description="Lists all classes (with at least one student enrolled) in the gradebook.")
    public String listClasses(){
        String query = "SELECT id_, course_, term_, section_ , description_, " +
                "COUNT(id_student_) AS student_count\n" +
                "FROM class_\n" +
                "    JOIN enrollment_ ON class_.id_ = enrollment_.id_class_\n" +
                "GROUP BY class_.id_, class_.term_\n" +
                "ORDER BY RIGHT(term_, 4) DESC;\n";

        String retString = "";
        String courseStr, semesterStr, sectionStr, descStr;
        int enrollmentInt;

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return activeClass + ": No classes yet.\n";
                }
                retString += "COURSE\tSEMESTER\tSECTION\tDESCRIPTION\tENROLLMENT\n";
                do{
                    courseStr = rs.getString("course_");
                    semesterStr = rs.getString("term_");
                    sectionStr = rs.getString("section_");
                    descStr = rs.getString("description_");
                    enrollmentInt = rs.getInt("student_count");
                    retString += courseStr + "\t\t" +
                            semesterStr + "\t\t" +
                            sectionStr + "\t\t" +
                            descStr + "\t\t" +
                            Integer.toString(enrollmentInt) + "\n";
                } while(rs.next());
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    /* variables to keep track of active class */
    private String activeClass = null;
    private int activeClass_id = -1;

    /* Commands for selecting class */
    @Command(description="Selects a class to interact with.")
    public String selectClass(String className) {
        String query = "SELECT id_, course_, term_, section_ , description_\n" +
                "FROM class_\n" +
                "WHERE course_ = ?\n" +
                "ORDER BY RIGHT(term_, 2) DESC\n" + // sorts by most recent
                "LIMIT 100;";

        String retString = "";
        int courseId = -1;
        String courseStr, termStr, sectionStr, descriptionStr;

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setString(1, className);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return className + ": class does not exist\n";
                }
                courseId  = rs.getInt("id_");
                courseStr = rs.getString("course_");
                termStr = rs.getString("term_");
                sectionStr = rs.getString("section_");
                descriptionStr = rs.getString("description_");
                if(rs.next()){
                    if (rs.getString("term_").equals(termStr)){
                        // Two courses exist in the most recent semester of this course
                        retString = "Error: More than one ["+className+"] exists in the most recent term.\n" +
                                    "Please specify a section number.";
                    } else {
                        // only one course exits in the most recent semester of this course.
                        activeClass = courseStr + " " + termStr + " " + sectionStr;
                        activeClass_id = courseId;
                        retString = "Success: Course selected -> " + activeClass;
                    }
                }
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    @Command(description="Selects a class to interact with.")
    public String selectClass(String className, String semester) {
        String query = "SELECT id_, course_, term_, section_ , description_\n" +
                "FROM class_\n" +
                "WHERE course_ = ?\n" +
                "  AND term_ = ?\n" +
                "ORDER BY RIGHT(term_, 2) DESC\n" + // sorts by most recent year TODO: semester
                "LIMIT 100;";

        String retString = "";
        int courseId = -1;
        String courseStr, termStr, sectionStr, descriptionStr;

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setString(1, className);
            stmt.setString(2, semester);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return className + ": class does not exist\n";
                }
                courseId  = rs.getInt("id_");
                courseStr = rs.getString("course_");
                termStr = rs.getString("term_");
                sectionStr = rs.getString("section_");
                descriptionStr = rs.getString("description_");
                if(rs.next()){
                    if (rs.getString("term_").equals(termStr)){
                        // At least two courses exist in the most recent semester of this course
                        retString = "Error: More than one ["+className+"] exists in ["+semester+"].\n" +
                                "Please specify a section number.";
                    } else {
                        // only one course exits in the most recent semester of this course.
                        activeClass = courseStr + " " + termStr + " " + sectionStr;
                        activeClass_id = courseId;
                        retString = "Success: Course selected -> " + activeClass;
                    }
                }
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    @Command(description="Selects a class to interact with.")
    public String selectClass(String className, String semester, String section) {
        String query = "SELECT id_, course_, term_, section_ , description_\n" +
                "FROM class_\n" +
                "WHERE course_ = ?\n" +
                "  AND term_ = ?\n" +
                "  AND section_ = ?\n" +
                "ORDER BY RIGHT(term_, 2) DESC\n" + // sorts by most recent year TODO: semester
                "LIMIT 100;";

        String retString = "";
        int courseId = -1;
        String courseStr, termStr, sectionStr, descriptionStr;

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setString(1, className);
            stmt.setString(2, semester);
            stmt.setString(3, section);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return className + ": course does not exist\n";
                }
                courseId = rs.getInt("id_");
                courseStr = rs.getString("course_");
                termStr = rs.getString("term_");
                sectionStr = rs.getString("section_");
                descriptionStr = rs.getString("description_");
                // only one course exits in the most recent semester of this course.
                activeClass = courseStr + " " + termStr + " " + sectionStr;
                activeClass_id = courseId;
                retString = "Success: Course selected -> " + activeClass;
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    /* command to show the currently selected class */
    @Command(description="Shows the currently selected class name.")
    public String showClass() {
        if(isClassSelected()){
            return activeClass;
        } else{
            return noClassString;
        }
    }

    /* Category and Item Management for active class */
    @Command(description="Shows grading categories from the active class.")
    public String showCategories() {
        if(!isClassSelected()){
            return noClassString;
        }
        String query = "SELECT id_, id_class_, name_, weight_\n" +
                        "FROM category_\n" +
                        "WHERE id_class_ = ?\n";

        String retString = "";
        String nameStr;
        double weight;

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setInt(1, activeClass_id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return activeClass + ": course has no categories.\n";
                }
                retString += "CATEGORY\tWEIGHT\n";
                do{
                    nameStr = rs.getString("name_");
                    weight = rs.getDouble("weight_");
                    retString += nameStr + "\t\t" + Double.toString(weight) + "\n";
                } while(rs.next());
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    @Command(description="Adds a new category for the active class.")
    public String addCategory(String name, double weight) {
        if(!isClassSelected()){
            return noClassString;
        }
        String insertCategory = "INSERT INTO category_ (id_class_, name_, weight_) VALUES (?, ?, ?);";
        int categoryId;
        try {
            db.setAutoCommit(false);
            PreparedStatement stmt = db.prepareStatement(insertCategory, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, activeClass_id);
            stmt.setString(2, name);
            stmt.setDouble(3, weight);
            stmt.executeUpdate();
//          ResultSet rs = stmt.executeQuery();
            db.commit();
        } catch (SQLException | RuntimeException e) {
            try{
                db.rollback();
            } catch (Exception E){
                return "SQL ERROR: " + e.toString() + E.toString();
            }
            return "SQL ERROR: " + e.toString();
            //throw e;
        } finally {
            try{
                db.setAutoCommit(true);
            } catch (Exception e){
                return "SQL ERROR: " + e.toString();
            }
        }
        return "Success";
    }

    @Command(description="Shows items from the active class, along with their categories, grouped by category.")
    public String showItems() {
        if(!isClassSelected()){
            return noClassString;
        }
        // item_ (id_, name_, point_max_, id_category_, description_)
        String query = "SELECT item_.name_ as item_name_, point_max_ as item_point_max_, category_.name_ as category_name_, category_.weight_ as category_weight_, description_ as item_description_\n" +
                "FROM item_\n" +
                "    JOIN category_ ON item_.id_category_ = category_.id_\n" +
                "WHERE category_.id_class_ = ?\n" +
                "GROUP BY category_.name_, item_.name_, point_max_, category_.weight_, description_;"
        ;
        String retString = "";
        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setInt(1, activeClass_id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return activeClass + ": course has no items.\n";
                }
                retString += "ITEM NAME\t\tMAX POINTS\t\tCATEGORY\t\tCAT-WEIGHT\t\tDESCRIPTION\n";
                do{
                    retString += rs.getString("item_name_") + "\t\t" +
                            rs.getString("item_point_max_") + "\t\t" +
                            rs.getString("category_name_") + "\t\t" +
                            rs.getString("category_weight_") + "\t\t" +
                            rs.getString("item_description_") + "\n";
                } while(rs.next());
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    @Command(description="Adds the given item to the gradebook.")
    public String addItem(String nameStr, String categoryStr, String descrStr, double maxPoint) {
        // item_ (id_, name_, point_max_, id_category_, description_)
        if(!isClassSelected()){
            return noClassString;
        }
        String insertCategory = "INSERT INTO item_ (name_, point_max_, id_category_, description_) VALUES (?, ?, ?, ?);";
        String queryCategory = "SELECT id_, id_class_, name_, weight_\n" +
                "FROM category_\n" +
                "WHERE id_class_ = ?\n" +
                "   AND name_ = ?\n";

        String retString = "";
        int categoryId;

        try (PreparedStatement catStmt = db.prepareStatement(queryCategory)) {
            catStmt.setInt(1, activeClass_id);
            catStmt.setString(2, categoryStr);
            try (ResultSet rs = catStmt.executeQuery()) {
                if (!rs.next()) {
                    //System.err.format("%d: class does not exist%n", className);
                    return activeClass + ": Category ["+categoryStr+"] does not exist for this course.\n";
                }
                categoryId = rs.getInt("id_");
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        try {
            db.setAutoCommit(false);
            PreparedStatement stmt = db.prepareStatement(insertCategory);
            stmt.setString(1, nameStr);
            stmt.setDouble(2, maxPoint);
            stmt.setInt(3, categoryId);
            stmt.setString(4, descrStr);
            stmt.executeUpdate();
//          ResultSet rs = stmt.executeQuery();
            db.commit();
        } catch (SQLException | RuntimeException e) {
            try{
                db.rollback();
            } catch (Exception E){
                return "SQL ERROR: " + e.toString() + E.toString();
            }
            return "SQL ERROR: " + e.toString();
            //throw e;
        } finally {
            try{
                db.setAutoCommit(true);
            } catch (Exception e){
                return "SQL ERROR: " + e.toString();
            }
        }
        return "Success";
    }

    /* Student-Class Management */
    @Command(description="Adds the student to the gradebook. Enrolls in active class. Updates name if necessary.")
    public String addStudent(String username, int studentId, String realName){
        // username studentid "Last, First"
        // adds a student and enrolls them in the current class.
        // If the student already exists, enroll them in the class;
        // if the name provided does not match their stored name,
        // update the name but print a warning that the name is being changed.
        if(!isClassSelected()){
            return noClassString;
        }
        // student_ (id_, username_, realname_)
        String queryStudent = "SELECT id_, username_, realname_\n" +
                "FROM student_\n" +
                "WHERE id_= ?;\n";
        String retString = "";

        // QUERY FOR STUDENT
        try (PreparedStatement studentStatement = db.prepareStatement(queryStudent)) {
            studentStatement.setInt(1, studentId);

            // FIRST CHECK FOR USER ID EXISTENCE
            try (ResultSet queryRs = studentStatement.executeQuery()) {
                /////////////////////////////////////////////////////////////////////////
                
                if (!queryRs.next()) {
                    
                    // STUDENT DOES NOT EXIST - ADD THEM.
                    
                    // TH: before student.id_ == 5001 does NOT exist.
                    // TH: insert_student_sub_ adds student to student_.
                    // TH: before class.id_ == 900 does NOT contain student (id_ = 5001).
                    // TH: insert_enrollment_sub_ adds student to enrollment_.
                    
                    String insert_string_ = 
                    "with " + 
                        "insert_student_sub_ as " + 
                            "( " + 
                                "insert " + 
                                    "into student_ " + 
                                        "(id_, username_, realname_) values (?, ?, ?) " + 
                                    "returning id_ " + 
                                        "as id_student_ " + 
                            ") " + 
                        ",grade_sub_ as (select 'X' as grade_default_) " + 
                        ",insert_enrollment_sub_ as " + 
                            "( " + 
                                "insert " + 
                                    "into " + 
                                        "enrollment_ " + 
                                            "(id_student_, id_class_, grade_) " + 
                                    "select " + 
                                        "id_student_, ?, grade_default_ " + 
                                    "from " + 
                                        "insert_student_sub_, grade_sub_ " + 
                            ") " + 
                        "select null " + 
                    ";"
                    ;
                    
                    try {
                        
                        db.setAutoCommit(false);
                        PreparedStatement insertStmt = db.prepareStatement(insert_string_);
                        insertStmt.setInt(1, studentId);
                        insertStmt.setString(2, username);
                        insertStmt.setString(3, realName);
                        insertStmt.setInt(4, activeClass_id);
                        insertStmt.executeQuery();
                        db.commit();
                        
                        ;
                        
                    }catch (SQLException | RuntimeException e) {
                        try{
                            db.rollback();
                        } catch (Exception E){
                            return "SQL ERROR, 0: " + e.toString() + E.toString();
                        }
                            return "SQL ERROR, 1: " + e.toString();
                            //throw e;
                    } finally {
                        try{
                            db.setAutoCommit(true);
                        } catch (Exception e){
                            return "SQL ERROR, 2: " + e.toString();
                        }
                    }
                    
                }//if student NOT found.
                
                else{
                    
                    // STUDENT ALREADY EXISTS - UPDATE IF NECESSARY.
                    
                    // TH: first things first -> goes to class.
                    String insert_string_ = 
                    "with " + 
                        "grade_sub_ as (select 'X' as grade_default_) " + 
                        ",insert_enrollment_sub_ as " + 
                            "( " + 
                                "insert " + 
                                    "into " + 
                                        "enrollment_ " + 
                                            "(id_student_, id_class_, grade_) " + 
                                    "select " + 
                                        "?, ?, grade_default_ " + 
                                    "from " + 
                                        "grade_sub_ " + 
                            ") " + 
                        "select null " + 
                    ";"
                    ;
                    
                    try {
                        
                        db.setAutoCommit(false);
                        PreparedStatement insertStmt = db.prepareStatement(insert_string_);
                        insertStmt.setInt(1, studentId);
                        insertStmt.setInt(2, activeClass_id);
                        insertStmt.executeQuery();
                        db.commit();
                        
                        ;
                        
                    }catch (SQLException | RuntimeException e) {
                        try{
                            db.rollback();
                        } catch (Exception E){
                            return "SQL ERROR, 3: " + e.toString() + E.toString();
                        }
                            System.out.println( "SQL ERROR, 4: " + e.toString() + " -> EXCEPTION IGNORED FOR NOW.");
                            //throw e;
                    } finally {
                        try{
                            db.setAutoCommit(true);
                        } catch (Exception e){
                            return "SQL ERROR, 5: " + e.toString();
                        }
                    }
                    
                    // TH: second things next -> updates student's name(s).
                    
                    String realNameDB = queryRs.getString("realname_");
                    String usernameDB = queryRs.getString("username_");
                    
                    if( !realNameDB.equals(realName) && !usernameDB.equals(username) ){
                        
                        System.out.println("WARNING: real name mismatch - updating database with new real name ...");
                        System.out.println("WARNING: user name mismatch - updating database with new user name ...");
                        
                        String update_string_ = 
                        "with " + 
                            "update_student_sub_ as " + 
                                "( " + 
                                    "update " + 
                                        "student_ " + 
                                    "set " + 
                                        "realname_ = ? " + 
                                        ",username_ = ? " + 
                                    "where " + 
                                        "id_ = ? " + 
                                ") " + 
                            "select null " + 
                        ";"
                        ;
                        
                        try {
                            
                            db.setAutoCommit(false);
                            PreparedStatement insertStmt = db.prepareStatement(update_string_);
                            insertStmt.setString(1, realName);
                            insertStmt.setString(2, username);
                            insertStmt.setInt(3,studentId);
                            insertStmt.executeQuery();
                            db.commit();
                            
                            ;
                            
                        }catch (SQLException | RuntimeException e) {
                            try{
                                db.rollback();
                            } catch (Exception E){
                                return "SQL ERROR: " + e.toString() + E.toString();
                            }
                                return "SQL ERROR: " + e.toString();
                                //throw e;
                        } finally {
                            try{
                                db.setAutoCommit(true);
                            } catch (Exception e){
                                return "SQL ERROR: " + e.toString();
                            }
                        }
                        
                        System.out.println("WARNING: real name updated.");
                        System.out.println("WARNING: user name updated.");
                        return "Success";
                        
                    }//if mismatched, 2.
                    
                    if( !realNameDB.equals(realName) ){
                        
                        System.out.println("WARNING: real name mismatch - updating database with new real name ...");
                        
                        String update_string_ = 
                        "with " + 
                            "update_student_sub_ as " + 
                                "( " + 
                                    "update " + 
                                        "student_ " + 
                                    "set " + 
                                        "realname_ = ? " + 
                                    "where " + 
                                        "id_ = ? " + 
                                ") " + 
                            "select null " + 
                        ";"
                        ;
                        
                        try {
                            
                            db.setAutoCommit(false);
                            PreparedStatement insertStmt = db.prepareStatement(update_string_);
                            insertStmt.setString(1, realName);
                            insertStmt.setInt(2,studentId);
                            insertStmt.executeQuery();
                            db.commit();
                            
                            ;
                            
                        }catch (SQLException | RuntimeException e) {
                            try{
                                db.rollback();
                            } catch (Exception E){
                                return "SQL ERROR: " + e.toString() + E.toString();
                            }
                                return "SQL ERROR: " + e.toString();
                                //throw e;
                        } finally {
                            try{
                                db.setAutoCommit(true);
                            } catch (Exception e){
                                return "SQL ERROR: " + e.toString();
                            }
                        }
                        
                        System.out.println("WARNING: real name updated.");
                        return "Success";
                        
                    }//if mismatched, 1: realName.
                    
                    if( !usernameDB.equals(username) ){
                        
                        System.out.println("WARNING: user name mismatch - updating database with new user name ...");
                        
                        String update_string_ = 
                        "with " + 
                            "update_student_sub_ as " + 
                                "( " + 
                                    "update " + 
                                        "student_ " + 
                                    "set " + 
                                        "username_ = ? " + 
                                    "where " + 
                                        "id_ = ? " + 
                                ") " + 
                            "select null " + 
                        ";"
                        ;
                        
                        try {
                            
                            db.setAutoCommit(false);
                            PreparedStatement insertStmt = db.prepareStatement(update_string_);
                            insertStmt.setString(1, username);
                            insertStmt.setInt(2,studentId);
                            insertStmt.executeQuery();
                            db.commit();
                            
                            ;
                            
                        }catch (SQLException | RuntimeException e) {
                            try{
                                db.rollback();
                            } catch (Exception E){
                                return "SQL ERROR: " + e.toString() + E.toString();
                            }
                                return "SQL ERROR: " + e.toString();
                                //throw e;
                        } finally {
                            try{
                                db.setAutoCommit(true);
                            } catch (Exception e){
                                return "SQL ERROR: " + e.toString();
                            }
                        }
                        
                        System.out.println("WARNING: user name updated.");
                        return "Success";
                        
                    }//if mismatched, 1: username.
                    
                    return "Success: Student Already Enrolled, NO Name Mismatched.";
                    
                }//else student found.
                
                /////////////////////////////////////////////////////////////////////////
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        return "Success";
    }

    @Command(description="Adds the student to the gradebook.")
    public String addStudent(String username){
        // add student to selected class
        if(!isClassSelected()){
            return noClassString;
        }
        String queryStudent = "SELECT id_, username_, realname_\n" +
                "FROM student_\n" +
                "WHERE username_= ?;\n";
        String retString = "";
        int studentId;

        // QUERY FOR STUDENT TO CONFIRM EXISTENCE
        try (PreparedStatement studentStatement = db.prepareStatement(queryStudent)) {
            studentStatement.setString(1, username);
            // FIRST CHECK FOR USER ID EXISTENCE
            ResultSet queryRs = studentStatement.executeQuery();
            if (!queryRs.next()) {
                // Student doesn't exist - return error
                return "Error: Student with username ["+username+"] does not exist.\n";
            } else{
                // STUDENT ALREADY EXISTS - CONTINUE TO ENROLLMENT STEP
                studentId = queryRs.getInt("id_");
            }
        } catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }

        // NOW ADD STUDENT TO ENROLLMENT LIST
        /////////////////////////////////////////////////////////////////////////
        String insert_string_ = 
        "with " + 
            "grade_sub_ as (select 'X' as grade_default_) " + 
            ",insert_enrollment_sub_ as " + 
                "( " + 
                    "insert " + 
                        "into " + 
                            "enrollment_ " + 
                                "(id_student_, id_class_, grade_) " + 
                        "select " + 
                            "?, ?, grade_default_ " + 
                        "from " + 
                            "grade_sub_ " + 
                ") " + 
            "select null " + 
        ";"
        ;
        
        try {
            db.setAutoCommit(false);
            PreparedStatement stmt = db.prepareStatement(insert_string_);
            stmt.setInt(1, studentId);
            stmt.setInt(2, activeClass_id);
            stmt.executeQuery();
            db.commit();
        } catch (SQLException | RuntimeException e) {
            try{
                db.rollback();
            } catch (Exception E){
                return "SQL ERROR, 0: " + e.toString() + E.toString();
            }
            return "SQL ERROR, 1: " + e.toString() + " -> Given Student Already Enrolled In Selected Class.";
        } finally {
            try{
                db.setAutoCommit(true);
            } catch (Exception e){
                return "SQL ERROR, 2: " + e.toString();
            }
        }
        /////////////////////////////////////////////////////////////////////////
        return "Success";
    }

    @Command(description="Lists students currently enrolled in the actively selected class.")
    public String showStudents(){
        if(!isClassSelected()){
            return noClassString;
        }
        // show enrolled students
        String query = "SELECT student_.id_, student_.username_, student_.realname_, enrollment_.grade_\n" +
                "FROM class_\n" +
                "    JOIN enrollment_ ON class_.id_ = enrollment_.id_class_\n" +
                "    JOIN student_ ON enrollment_.id_student_ = student_.id_\n" +
                "WHERE class_.id_ = ?\n" +
                "GROUP BY student_.id_, student_.username_, enrollment_.grade_\n" +
                "ORDER BY student_.username_ DESC\n" +
                "LIMIT 200;";

        String retString = "";

        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setInt(1, activeClass_id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return activeClass + ": course has no students.\n";
                }
                retString += "STUDENT ID\tUSERNAME\t\tNAME\tGRADE\n";
                do{
                    retString +=  rs.getInt("id_") + "\t\t" +
                            rs.getString("username_") + "\t\t" +
                            rs.getString("realname_") + "\t\t" +
                            rs.getString("grade_") + "\n";
                } while(rs.next());
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;
    }

    @Command(description="Shows student names (of given class) with given pattern in their username or realname.")
    public String showStudents(String pattern){
        // show enrolled students
        if(!isClassSelected()){
            return noClassString;
        }
        String query = "SELECT student_.id_, student_.username_, student_.realname_, enrollment_.grade_\n" +
                "FROM class_\n" +
                "         JOIN enrollment_ ON class_.id_ = enrollment_.id_class_\n" +
                "         JOIN student_ ON enrollment_.id_student_ = student_.id_\n" +
                "WHERE class_.id_ = ?\n" +
                "  AND (LOWER(student_.realname_) LIKE ?\n" +
                "    OR LOWER(student_.username_) LIKE ?)\n" +
                "GROUP BY student_.id_, student_.username_, enrollment_.grade_\n" +
                "ORDER BY student_.username_ DESC\n" +
                "LIMIT 200;";

        pattern = pattern.toLowerCase();
        pattern = "%" + pattern + "%";
        String retString = "";
        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setInt(1, activeClass_id);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return activeClass + ": course has NO match for this pattern.\n";
                }
                retString += "STUDENT ID\tUSERNAME\t\tNAME\tGRADE\n";
                do{
                    retString +=  rs.getInt("id_") + "\t\t" +
                            rs.getString("username_") + "\t\t" +
                            rs.getString("realname_") + "\t\t" +
                            rs.getString("grade_") + "\n";
                } while(rs.next());
            }
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        return retString;    }

    @Command(description="Assigns particular grade value to given student on given item.")
    public String grade(String itemName, String username, double score){
	// If the student already has a grade for that item, replace it.
	// If the number of points exceeds the number of points configured for the item,
	// print a warning (showing the number of points configured).
	
        if(!isClassSelected()){
            return noClassString;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
	// TH: STEP 1 --- checks for student-item compatibility -> if yes, grade assignment ready ... else, errors.
	// TH: checks for student-item compatibility.
	// TH: retrieves item ID if given item exists (by name),
	// TH: from all items from all categories from all classes enrolled by given student.
	// TH: module A10.
        
	String query_base_ = 
        "with " +
	"get_id_student_ as " +
		"( " +
			"select " +
				"student_.id_ as id_student_ " +
			"from " +
				"student_ " +
			"where " +
				"student_.username_ = ? " +
		") " +
	",get_id_class_ as " +
		"( " +
			"select " +
				"id_class_ as id_class__ " +
			"from " +
				"enrollment_ " +
			"where " +
				"id_student_ in " +
				"( " +
					"select * from get_id_student_ " +
				") " +
		") " +
	",select_category_ as " +
		"( " +
			"select " +
				"category_.id_ as id_category__ " +
				",category_.name_ as name_category_ " +
				",category_.weight_ as weight_category_ " +
				",category_.id_class_ as class_category_ " +
			"from " +
				"category_ " +
				",get_id_class_ " +
			"where " +
				"category_.id_class_ = id_class__ " +
		") " 
        ;
        
	String query_student_item_compatible_ = 
        "select " +
		"id_student_ " +
		",item_.id_ as id_item_ " +
		",item_.point_max_ as item_point_max_ " + 
	"from " +
		"item_ " +
		",select_category_ " +
		",get_id_student_ " +
	"where " +
		"id_category_ = id_category__ " +
		"AND item_.name_ = ? " +
        "; " 
        ;
        
        String id_student_ = null;
        String id_item_    = null;
        double item_point_max_ = -1;
        try (PreparedStatement stmt = db.prepareStatement(query_base_+query_student_item_compatible_)) {
           
           // TH: sets parameters for query.
           stmt.setString(1, username);
           stmt.setString(2, itemName);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { return "ERROR --- student-item NOT compatible."; }
              
              // TH: gets column values from query.
              id_student_ = rs.getString("id_student_");
              id_item_    = rs.getString("id_item_");
              item_point_max_ = rs.getDouble("item_point_max_");
              
              // TH: returns for debug.
//            return "[grade]: id_student_ == " + id_student_ + ", id_item_ == " + id_item_ + ".";
              
           }//try.
           
        }//try.
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // TH: STEP 2 --- checks if item grade already assigned -> if yes, update ... else insert.
        
        String query_item_grade_ =
        "select count(*) as count_item_grade_ " +
        "from item_grade_ " +
        "where " +
        "id_student_ = ? " +
        "and id_item_ = ? " +
        ";"
        ;
        
        int count_item_grade_ = 0;
        try (PreparedStatement stmt = db.prepareStatement(query_item_grade_)) {
           
           // TH: sets parameters for query.
           stmt.setInt(1, Integer.parseInt(id_student_));
           stmt.setInt(2, Integer.parseInt(id_item_));
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              count_item_grade_= rs.getInt("count_item_grade_");
              
           }//try.
           
        }//try.
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        // TH: returns for debug.
//      return "[grade]: count_item_grade_ == " + count_item_grade_ + ".";
        
        if( count_item_grade_ == 0 ) {
                 
                 String insert_item_grade_ =
                 "insert " + 
                    "into " + 
                       "item_grade_ " + 
                          "(id_student_, id_item_, point_actual_) " + 
                       "select " + 
                          "?, ?, ? " +  
                 ";"
                 ;
                 
                 try{
                    db.setAutoCommit(false);
                    PreparedStatement stmt = db.prepareStatement(insert_item_grade_);
                    stmt.setInt(1, Integer.parseInt(id_student_));
                    stmt.setInt(2, Integer.parseInt(id_item_));
                    stmt.setDouble(3, score);
                    stmt.executeUpdate();
                    db.commit();
                 } catch (SQLException | RuntimeException e) {
                    try{
                       db.rollback();
                    } catch (Exception E){
                       // rollback failed
                       return "SQL ERROR: " + e.toString() + E.toString();
                    }
                    return "SQL ERROR: " + e.toString();
                    //throw e;
                 } finally {
                    try{
                       db.setAutoCommit(true);
                    } catch (Exception e){
                       // autocommit switch failed
                       return "SQL ERROR: " + e.toString();
                    }
                 }
                 
                 System.out.println("[grade]: item grade inserted.");
                 
        }//if zero.
        
        else if( count_item_grade_ == 1 ) {
                 
                 String update_item_grade_ = 
                 "update " + 
                    "item_grade_ " + 
                 "set " + 
                    "point_actual_ = ? " + 
                 "where " + 
                    "id_student_  = ? " + 
                    "AND id_item_ = ? " + 
                 "; "
                 ;
                 
                 try{
                    db.setAutoCommit(false);
                    PreparedStatement stmt = db.prepareStatement(update_item_grade_);
                    stmt.setDouble(1, score);
                    stmt.setInt(2, Integer.parseInt(id_student_));
                    stmt.setInt(3, Integer.parseInt(id_item_));
                    stmt.executeUpdate();
                    db.commit();
                 } catch (SQLException | RuntimeException e) {
                    try{
                       db.rollback();
                    } catch (Exception E){
                       // rollback failed
                       return "SQL ERROR: " + e.toString() + E.toString();
                    }
                    return "SQL ERROR: " + e.toString();
                    //throw e;
                 } finally {
                    try{
                       db.setAutoCommit(true);
                    } catch (Exception e){
                       // autocommit switch failed
                       return "SQL ERROR: " + e.toString();
                    }
                 }
                 
                 System.out.println("[grade]: item grade updated.");
                 
        }//else one.
        
        else{
           System.out.println("[grade]: hopefully never happens :( ");
        }//else.
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        if(item_point_max_<score) System.out.println("[grade]: WARNING --- maximum configured point (" + item_point_max_ + ") exceeded.");
	return "Success";
    }

    @Command(description="Calculates grade(s) for given student in given class.")
    public String studentGrades(String username){
        // show student’s current grade: all items, visually grouped by category,
        // with the student’s grade (if they have one). Show subtotalsfor each category,
        // along with the overall grade in the class.
        
        if(!isClassSelected()){
            return noClassString;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        String query_enrollment_ = 
        // TH: retrieves all classes enrolled by given student.
        "select " + 
           "* " + 
        "from " + 
           "enrollment_ " + 
        "where " + 
           "id_student_ in " + 
           "( " + 
              "select " + 
                 "student_.id_ " + 
              "from " + 
                 "student_ " + 
              "where " + 
                 "student_.username_ = ? " + 
           ") " + 
        "; "
        ;
        
        try (PreparedStatement stmt = db.prepareStatement(query_enrollment_)) {
           
           boolean incompatible_ = true;
           
           stmt.setString(1,username);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              
              do {
                 
                 if(rs.getInt("id_class_") == activeClass_id) {
                    incompatible_=false;
                    break;
                 }//if enrollment record found.
                 
              } while(rs.next());
              
           }
           
           if(incompatible_==true){
              return "Failure (No Enrollment Record Found).";
           }//if incompatible_.
           
        }
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("[studentGrades]: username                                == " + username + ".");
        System.out.println("[studentGrades]: activeClass_id                          == " + activeClass_id + ".");
        
        String select_count_item_              = "select " + "count(*) as count_item_per_student_per_class_ " ;
        String select_count_item_grade_        = "select " + "count(*) as count_item_grade_per_student_per_class_ " ;
        String select_pair_student_item_       = "select " + "* " ;
        String select_pair_student_item_grade_ = "select " + "* " ;
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // TH: STEP 1: retrieves count of all items from all categories in given class enrolled by given student.
        // TH: STEP 2: retrieves number of records for all items for given student in given class, grouped by category.
        
        // TH: STEP 3:  if NO mismatch: count of all items (STEP 1) == number of records for all items (STEP 2) -> goes to STEP 5.
        
        // TH: STEP 4:  consults debug_A4b_sample_.png for (id_student_, item_id_) combo -> module A4b.
        //     STEP 4a: lists what items given student has to deal with in given class.
        //     STEP 4b: assigns default grade to each (id_student_, item_id_) combo NOT found in item_grade_ (i.e. deploys A4a, debug_item_grade_count_.png).
        //     STEP 4c: retrieves count of all items from all categories in given class enrolled by given student.
        //     STEP 4d: retrieves number of records for all items for given student in given class, grouped by category.
        //     STEP 4e: if NO mismatch: count of all items (STEP 4c) == number of records for all items (STEP 4d) -> goes to STEP 5.
        
        // TH: STEP 5:  calculates grade for given student in given class.
        //    [STEP 5a: debug_grade_calc_numeric_per_category_.png OR debug_grade_calc_numeric_per_class_.png]
        //     STEP 5b: debug_grade_calc_letter_per_category_.png  OR debug_grade_calc_letter_per_class_.png
        //     STEP 5c: returns "Success";
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // STEP 1.
        
        String query_step_1_ = select_count_item_;

String query_step_1_from_ =
"from " +
	"( " +
		// IMPORTANT: retrieves all items from all categories in given class enrolled by given student.
		// sorted by category, and by item name.
		// TH: retrieves all categories from given class enrolled by given student.
		// TH: retrieves all items for given (recently retrieved) category IDs.
		// TH: module A0+A1 -> A4.
		"with " +
			"get_id_class_ as  " +
				"( " +
					"select " +
						"id_class_ as id_class__ " +
						",id_student_ " +
					"from " +
						"enrollment_ " +
					"where " +
						"id_student_ in " +
						"( " +
							"select " +
								"student_.id_ " +
							"from " +
								"student_ " +
							"where " +
								"student_.username_ = ? " +
						") " +
				") " +
			",select_category_ as " +
				"( " +
					"select " +
						"id_student_ " +
						",category_.id_ as id_category__ " +
						",category_.name_ as name_category_ " +
						",category_.weight_ as weight_category_ " +
						",category_.id_class_ as class_category_ " +
					"from " +
						"category_ " +
						",get_id_class_ " +
					"where " +
						"category_.id_class_ = id_class__ " +
						"AND id_class__ = ? " +
				") " +
			"select " +
				"id_student_ " +
			//	",class_category_ as class_id_ " +
			//	",item_.id_category_ as category_id_ " +
				",item_.id_ as item_id_ " +
			//	",item_.name_ as item_name_ " +
			//	",item_.point_max_ as item_point_max_ " +
			//	",item_.description_ as item_description_ " +
			"from " +
				"item_ " +
				",select_category_ " +
			"where " +
				"id_category_ = id_category__ " +
			"order by " +
				"id_category_ " +
			//	",item_name_ " +
	")module_A4a " +
"; "
;
        ;
        
        query_step_1_ += query_step_1_from_;
        
        int count_item_per_student_per_class_ = 0;
        try (PreparedStatement stmt = db.prepareStatement(query_step_1_)) {
           
           stmt.setString(1,username);
           stmt.setInt(2,activeClass_id);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              count_item_per_student_per_class_ = rs.getInt("count_item_per_student_per_class_");
              
              System.out.println("[studentGrades]: count_item_per_student_per_class_ (Before)       == " + count_item_per_student_per_class_ + ".");
              
           }
           
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // STEP 2.
        
        String query_step_2_ = select_count_item_grade_;
// TH: experimental: gets ready for grade calculation for given student in given class.
// TH: retrieves number of records for all items for given student in given class, grouped by category.
// TH: module A5a.

String query_step_2_from_ =
"from " + 
	"( " + 
		// IMPORTANT: lists records for all items for given student in given class, grouped by category.
		// TH: module A2+A3 -> A5.
		// TH: CTE version.

				// TH: retrieves all categories from all classes enrolled by given student.
				// TH: retrieves all items for given (recently retrieved) category IDs.
				// TH: module A0+A1 -> A2.
				// TH: retrieves ONLY row(s) for given class (id_: 330) -> item_.id_: 600, 185, 520, 736, 34.
				"with " + 
					"get_id_student_ as " + 
						"( " + 
							"select " + 
								"student_.id_ as id_student_ " + 
							"from " + 
								"student_ " + 
							"where " + 
								"student_.username_ = ? " + 
						") " + 
					",get_id_class_ as  " + 
						"( " + 
							"select " + 
								"id_class_ as id_class__ " + 
								",id_student_ as id_student__ " + 
							"from " + 
								"enrollment_ " + 
							"where " + 
								"id_student_ in " + 
								"( " + 
									"select * from get_id_student_ " + 
								") " + 
						") " + 
					",select_category_ as " + 
						"( " + 
							"select " + 
								"id_class__ as id_class___ " + 
								",category_.id_ as id_category__ " + 
								",category_.name_ as name_category_ " + 
								",category_.weight_ as weight_category_ " + 
								",category_.id_class_ as class_category_ " + 
							"from " + 
								"category_ " + 
								",get_id_class_ " + 
							"where " + 
								"category_.id_class_ = id_class__ " + 
								"AND id_class__ = ? " + 
						") " + 
					",select_category_item_components as " + 
						"( " + 
							"select  " + 
								"id_class___ " + 
								",item_.id_ as item_id_ " + 
								",item_.name_ as item_name_ " + 
								",item_.point_max_ as item_point_max_ " + 
								",item_.id_category_ as item_id_category_ " + 
								",item_.description_ as item_description_ " + 
								",name_category_ " + 
								",weight_category_ " + 
							"from " + 
								"item_ " + 
								",select_category_ " + 
							"where " + 
								"id_category_ = id_category__ " + 
						") " + 
					// TH: retrieves actual point per item (i.e. by ID) for given student.
					// TH: module A3.
					"select " + 
						"get_id_student_.id_student_ as id_student__ " + 
					//	",id_class___ " + 
					//	",item_id_category_ as id_category_ " + 
					//	",name_category_ " + 
					//	",weight_category_ " + 
						",item_id_ " + 
					//	",item_name_ " + 
					//	",item_description_ " + 
						",point_actual_ " + 
					//	",item_point_max_ " + 
					"from  " + 
						"item_grade_ " + 
						",select_category_item_components " + 
						",get_id_student_ " + 
					"where " + 
						"item_grade_.id_student_  = get_id_student_.id_student_ " + 
						"AND  " + 
							"( " + 
								// TH: receives output data from earlier subquery.
								"id_item_ = item_id_ " + 
							") " + 
					"group by " + 
					//	"id_category_ " + 
					//	",weight_category_ " + 
						"id_student__ " + 
					//	",id_class___ " + 
						",item_id_ " + 
					//	",item_name_ " + 
					//	",name_category_ " + 
						",point_actual_ " + 
					//	",item_point_max_ " + 
					//	",item_description_ " + 
					"order by " + 
						"point_actual_ asc " + 
	")module_A5a " + 
"; "
;
        ;
        
        query_step_2_ += query_step_2_from_;
        
        int count_item_grade_per_student_per_class_ = 0;
        try (PreparedStatement stmt = db.prepareStatement(query_step_2_)) {
           
           stmt.setString(1,username);
           stmt.setInt(2,activeClass_id);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              count_item_grade_per_student_per_class_ = rs.getInt("count_item_grade_per_student_per_class_");
              
              System.out.println("[studentGrades]: count_item_grade_per_student_per_class_ (Before) == " + count_item_grade_per_student_per_class_ + ".");
              
           }
           
        }catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // STEP 3: if item == item_grade_.
        
        if( count_item_per_student_per_class_ < count_item_grade_per_student_per_class_ ) {
           System.out.println("[studentGrades]: count_item_per_student_per_class_ < count_item_grade_per_student_per_class.");
           System.out.println("[studentGrades]: Ended (Error).");
        }//if item < item_grade_.
        
        else if( count_item_per_student_per_class_ > count_item_grade_per_student_per_class_ ) {
           
           // STEP 4.
           
           //     STEP 4a: lists what items given student has to deal with in given class.
           query_step_1_ = select_pair_student_item_ + query_step_1_from_;
           
           try (PreparedStatement stmt = db.prepareStatement(query_step_1_)) {
              
              stmt.setString(1,username);
              stmt.setInt(2,activeClass_id);
              
              try (ResultSet rs = stmt.executeQuery()) {
                 
                 if (!rs.next()) {
                    return "[studentGrades]: There are NO items in selected class :( ";
                 }
                 
                 do{
                    
                    // TH: walks thru all (student-item) pairs to check for missing item grades.
                    int id_student_ = rs.getInt("id_student_");
                    int id_item_    = rs.getInt("item_id_");
                    
                    String query_count_item_grade_ = "select count(*) as count_item_grade_ from item_grade_ where id_student_ = ? AND id_item_ = ? ;" ;
                    
                    try (PreparedStatement stmt_ = db.prepareStatement(query_count_item_grade_)) {
                       
                       stmt_.setInt(1,id_student_);
                       stmt_.setInt(2,id_item_);
                       
                       try (ResultSet rs_ = stmt_.executeQuery()) {
                          
                          if (!rs_.next()) {
                             return "[studentGrades]: hopefully new happens :( ";
                          }
                          
                          int count_item_grade_ = rs_.getInt("count_item_grade_");
                          
                          // TH: if given (student,item) pair does NOT have item grade already assigned.
                          if(count_item_grade_==0) {
                             
//                           System.out.println("id_student_\t\titem_id_ (NOT in item_grade_)");
//                           System.out.println(id_student_+"\t\t\t"+id_item_);
                             
                             //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                             
                             //     STEP 4b: assigns default grade to each (id_student_, item_id_) combo NOT found in item_grade_ (i.e. deploys A4a, debug_item_grade_count_.png).
                             
                             if( true ) {
                                
                                String insert_item_grade_default_ =
                                "with " + 
                                "get_id_student_ as (select ? as id_student_) " +
                                ",get_id_item_ as (select ? as id_item_) " +
                                ",get_point_actual_default_ as (select 0.0 as point_actual_default_) " + 
                                "insert " + 
                                   "into " + 
                                      "item_grade_ " + 
                                         "(id_student_, id_item_, point_actual_) " + 
                                "select " + 
                                   "id_student_, id_item_, point_actual_default_ " +  
                                "from " + 
                                   "get_id_student_ " + 
                                   ",get_id_item_ " + 
                                   ",get_point_actual_default_ " +
                                ";"
                                ;
                                
                                try{
                                   
                                   db.setAutoCommit(false);
                                   PreparedStatement stmt__ = db.prepareStatement(insert_item_grade_default_);
                                   
                                   stmt__.setInt(1, id_student_);
                                   stmt__.setInt(2, id_item_);
                                   
                                   stmt__.executeUpdate();
                                   
                                   db.commit();
                                   
                                } catch (SQLException | RuntimeException e) {
                                      try{
                                         db.rollback();
                                      } catch (Exception E){
                                         // rollback failed
                                         return "SQL ERROR: " + e.toString() + E.toString();
                                      }
                                      return "SQL ERROR: " + e.toString();
                                      //throw e;
                                } finally {
                                      try{
                                         db.setAutoCommit(true);
                                      } catch (Exception e){
                                      // autocommit switch failed
                                      return "SQL ERROR: " + e.toString();
                                      }
                                }
                                
                                System.out.println("[studentGrades]: WARNING --- default item grade inserted (" + id_student_ + "," + id_item_ + ").");
                                
                             }//if true.
                             
                             //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                             
                          }
                          
                       }
                       
                    }
                    
                 } while(rs.next());
                 
              }
              
           }
           catch (SQLException sqEx){
              return "SQL ERROR: " + sqEx;
           }
           
           //     STEP 4c: retrieves count of all items from all categories in given class enrolled by given student.
           query_step_1_ = select_count_item_ + query_step_1_from_;
           count_item_per_student_per_class_ = 0;
           try (PreparedStatement stmt = db.prepareStatement(query_step_1_)) {
              stmt.setString(1,username);
              stmt.setInt(2,activeClass_id);
              try (ResultSet rs = stmt.executeQuery()) {
                 if (!rs.next()) { /* TH: hopefully never happens :( */ }
                 count_item_per_student_per_class_ = rs.getInt("count_item_per_student_per_class_");
                 System.out.println("[studentGrades]: count_item_per_student_per_class_ (After)        == " + count_item_per_student_per_class_ + ".");
              }
           }
           catch (SQLException sqEx){
               return "SQL ERROR: " + sqEx;
           }
           
           //     STEP 4d: retrieves number of records for all items for given student in given class, grouped by category.
           query_step_2_ = select_count_item_grade_ + query_step_2_from_;
           count_item_grade_per_student_per_class_ = 0;
           try (PreparedStatement stmt = db.prepareStatement(query_step_2_)) {
              stmt.setString(1,username);
              stmt.setInt(2,activeClass_id);
              try (ResultSet rs = stmt.executeQuery()) {
                 if (!rs.next()) { /* TH: hopefully never happens :( */ }
                 count_item_grade_per_student_per_class_ = rs.getInt("count_item_grade_per_student_per_class_");
                 System.out.println("[studentGrades]: count_item_grade_per_student_per_class_ (After)  == " + count_item_grade_per_student_per_class_ + ".");
              }
           }
           catch (SQLException sqEx){
               return "SQL ERROR: " + sqEx;
           }
           
           //     STEP 4e: if NO mismatch: count of all items (STEP 4c) == number of records for all items (STEP 4d) -> goes to STEP 5.
           if(count_item_per_student_per_class_ != count_item_grade_per_student_per_class_) return "FAILURE: UNKNOWN SQL ERROR.";
           else;
           
//         return "[studentGrades]: STILL IN DEVELOPMENT.";
           
        }//if item > item_grade_.
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // STEP 5b: category.
        
        String grade_letter_per_student_per_category_ = 
// TH: experimental -> gets ready for final version.
//// TH: calculates letter grade per category for given student in given class.
"select " + 
	"* " + 
	",case " + 
		"when grade_per_category_ >= 0.96 then 'A+' " + 
		"when grade_per_category_ >= 0.93 AND grade_per_category_ < 0.96 then 'A'  " + 
		"when grade_per_category_ >= 0.90 AND grade_per_category_ < 0.93 then 'A-' " + 
		"when grade_per_category_ >= 0.86 AND grade_per_category_ < 0.90 then 'B+' " + 
		"when grade_per_category_ >= 0.83 AND grade_per_category_ < 0.86 then 'B'  " + 
		"when grade_per_category_ >= 0.80 AND grade_per_category_ < 0.83 then 'B-' " + 
		"when grade_per_category_ >= 0.76 AND grade_per_category_ < 0.80 then 'C+' " + 
		"when grade_per_category_ >= 0.73 AND grade_per_category_ < 0.76 then 'C'  " + 
		"when grade_per_category_ >= 0.70 AND grade_per_category_ < 0.73 then 'C-' " + 
		"when grade_per_category_ >= 0.66 AND grade_per_category_ < 0.70 then 'D+' " + 
		"when grade_per_category_ >= 0.63 AND grade_per_category_ < 0.66 then 'D'  " + 
		"when grade_per_category_ >= 0.60 AND grade_per_category_ < 0.63 then 'D-' " + 
		"when grade_per_category_ >= 0.56 AND grade_per_category_ < 0.60 then 'E+' " + 
		"when grade_per_category_ >= 0.53 AND grade_per_category_ < 0.56 then 'E'  " + 
		"when grade_per_category_ >= 0.50 AND grade_per_category_ < 0.53 then 'E-' " + 
		"when grade_per_category_ >= 0.46 AND grade_per_category_ < 0.50 then 'F+' " + 
		"when grade_per_category_ >= 0.43 AND grade_per_category_ < 0.46 then 'F'  " + 
		"when grade_per_category_ >= 0.40 AND grade_per_category_ < 0.43 then 'F-' " + 
		"when grade_per_category_ >= 0.36 AND grade_per_category_ < 0.40 then 'G+' " + 
		"when grade_per_category_ >= 0.33 AND grade_per_category_ < 0.36 then 'G'  " + 
		"when grade_per_category_ >= 0.30 AND grade_per_category_ < 0.33 then 'G-' " + 
		"when grade_per_category_ >= 0.26 AND grade_per_category_ < 0.30 then 'H+' " + 
		"when grade_per_category_ >= 0.23 AND grade_per_category_ < 0.26 then 'H'  " + 
		"when grade_per_category_ >= 0.20 AND grade_per_category_ < 0.23 then 'H-' " + 
		"when grade_per_category_ >= 0.16 AND grade_per_category_ < 0.20 then 'I+' " + 
		"when grade_per_category_ >= 0.13 AND grade_per_category_ < 0.16 then 'I'  " + 
		"when grade_per_category_ >= 0.10 AND grade_per_category_ < 0.13 then 'I-' " + 
		"when grade_per_category_ >= 0.06 AND grade_per_category_ < 0.10 then 'J+' " + 
		"when grade_per_category_ >= 0.03 AND grade_per_category_ < 0.06 then 'J'  " + 
		"when grade_per_category_ <  0.03 then 'J-' " + 
	"end as letter_grade_per_category_ " + 
"from " + 
	"( " + 
//
		// TH: calculates numeric grade per category for given student in given class.
		// TH: A7+A8.
		//// TH: gets numerator and denominator components for grade calculation for given student in given class.
		"select " + 
			"* " + 
			", numerator_component_sum_per_category_ / denominator_component_per_category_ as grade_per_category_ " + 
		"from " + 
			"( " + 
				//
				// TH: gets subtotal per category, from all items for given student in given class.
				// TH: module A7.
				//// TH: gets numerator components from all items for given student in given class, grouped by category.
				//// TH: see debug_0_part2_.png for numerator sample data.
				//// TH: see debug_A6_.png for denominator sample data.
				"with " + 
					"get_numerator_components as " + 
						"( " + 
				//
							// IMPORTANT: lists records for all items for given student in given class, grouped by category.
							// TH: module A2+A3 -> A5.
							// TH: CTE version.

									// TH: retrieves all categories from all classes enrolled by given student.
									// TH: retrieves all items for given (recently retrieved) category IDs.
									// TH: module A0+A1 -> A2.
									// TH: retrieves ONLY row(s) for given class (id_: 330) -> item_.id_: 600, 185, 520, 736, 34.
									"with " + 
										"get_id_student_ as " + 
											"( " + 
												"select " + 
													"student_.id_ as id_student_ " + 
												"from " + 
													"student_ " + 
												"where " + 
													"student_.username_ = ? " + 
											") " + 
										",get_id_class_ as  " + 
											"( " + 
												"select " + 
													"id_class_ as id_class__ " + 
												"from " + 
													"enrollment_ " + 
												"where " + 
													"id_student_ in " + 
													"( " + 
														"select * from get_id_student_ " + 
													") " + 
											") " + 
										",select_category_ as " + 
											"( " + 
												"select " + 
													"id_class__ as id_class___ " + 
													",category_.id_ as id_category__ " + 
													",category_.name_ as name_category_ " + 
													",category_.weight_ as weight_category_ " + 
													",category_.id_class_ as class_category_ " + 
												"from " + 
													"category_ " + 
													",get_id_class_ " + 
												"where " + 
													"category_.id_class_ = id_class__ " + 
													"AND id_class__ = ? " + 
											") " + 
										",select_category_item_components as " + 
											"( " + 
												"select  " + 
													"id_class___ " + 
													",item_.id_ as item_id_ " + 
													",item_.name_ as item_name_ " + 
													",item_.point_max_ as item_point_max_ " + 
													",item_.id_category_ as item_id_category_ " + 
													",item_.description_ as item_description_ " + 
													",name_category_ " + 
													",weight_category_ " + 
												"from " + 
													"item_ " + 
													",select_category_ " + 
												"where " + 
													"id_category_ = id_category__ " + 
											") " + 
										// TH: retrieves actual point per item (i.e. by ID) for given student.
										// TH: module A3.
										"select " + 
											"get_id_student_.id_student_ as id_student_ " + 
											",id_class___ " + 
											",item_id_category_ as id_category_ " + 
											",name_category_ " + 
											",weight_category_ " + 
											",item_id_ " + 
											",item_name_ " + 
											",item_description_ " + 
											",point_actual_ " + 
											",item_point_max_ " + 
										"from  " + 
											"item_grade_ " + 
											",select_category_item_components " + 
											",get_id_student_ " + 
										"where " + 
											"item_grade_.id_student_  = get_id_student_.id_student_ " + 
											"AND  " + 
												"( " + 
													// TH: receives output data from earlier subquery.
													"id_item_ = item_id_ " + 
												") " + 
										"group by " + 
											"id_category_ " + 
											",weight_category_ " + 
											",get_id_student_.id_student_ " + 
											",id_class___ " + 
											",item_id_ " + 
											",item_name_ " + 
											",name_category_ " + 
											",point_actual_ " + 
											",item_point_max_ " + 
											",item_description_ " + 
										"order by " + 
											"point_actual_ asc " + 
				//
						") " + 
					"select  " + 
						"id_student_ " + 
						",id_category_ " + 
						",id_class___ " + 
						",sum(point_actual_ / item_point_max_ * weight_category_) as numerator_component_sum_per_category_ " + 
					"from " + 
						"get_numerator_components " + 
					"group by " + 
						"id_category_ " + 
						",id_student_ " + 
						",id_class___ " + 
		//
			")module_A7_ " + 
			"inner join " + 
			"( " + 
		//
				// TH: gets denominator components from all category instances for given student in given class.
				// TH: module A8.
				// TH: see debug_A6_.png for denominator sample data.
				// TH: see debug_0_part2_.png for numerator sample data.
				"with " + 
					"get_denominator_components as " + 
						"( " + 
				//
							// TH: retrieves count of instances per category along with its weight,
							// TH: from given class enrolled by given student.
							// TH: module A6 -> represents denominator in grade calculation per student per class.
							//// IMPORTANT: lists records for all items for given student in given class, grouped by category.
							//// TH: module A2+A3 -> A5.
							//// TH: CTE version.

									// TH: retrieves all categories from all classes enrolled by given student.
									// TH: retrieves all items for given (recently retrieved) category IDs.
									// TH: module A0+A1 -> A2.
									// TH: retrieves ONLY row(s) for given class (id_: 330) -> item_.id_: 600, 185, 520, 736, 34.
									"with " + 
										"get_id_student_ as " + 
											"( " + 
												"select " + 
													"student_.id_ as id_student_ " + 
												"from " + 
													"student_ " + 
												"where " + 
													"student_.username_ = ? " + 
											") " + 
										",get_id_class_ as  " + 
											"( " + 
												"select " + 
													"id_class_ as id_class__ " + 
												"from " + 
													"enrollment_ " + 
												"where " + 
													"id_student_ in " + 
													"( " + 
														"select * from get_id_student_ " + 
													") " + 
											") " + 
										",select_category_ as " + 
											"( " + 
												"select " + 
													"id_class__ as id_class___ " + 
													",category_.id_ as id_category__ " + 
													",category_.name_ as name_category_ " + 
													",category_.weight_ as weight_category_ " + 
													",category_.id_class_ as class_category_ " + 
												"from " + 
													"category_ " + 
													",get_id_class_ " + 
												"where " + 
													"category_.id_class_ = id_class__ " + 
													"AND id_class__ = ? " + 
											") " + 
										",select_category_item_components as " + 
											"( " + 
												"select  " + 
													"id_class___ " + 
													",item_.id_ as item_id_ " + 
													",item_.name_ as item_name_ " + 
													",item_.point_max_ as item_point_max_ " + 
													",item_.id_category_ as item_id_category_ " + 
													",item_.description_ as item_description_ " + 
													",name_category_ " + 
													",weight_category_ " + 
												"from " + 
													"item_ " + 
													",select_category_ " + 
												"where " + 
													"id_category_ = id_category__ " + 
											") " + 
										// TH: retrieves count of instances per category along with its weight,
										// TH: from given class enrolled by given student.
										"select " + 
											"item_id_category_ as id_category_ " + 
											",count(name_category_) as count_name_category_ " + 
											",weight_category_ " + 
											",get_id_student_.id_student_ as id_student_ " + 
											",id_class___ " + 
										//	",item_id_category_ as id_category_ " + 
										//	",item_id_ " + 
										//	",item_name_ " + 
										//	",item_description_ " + 
										//	",point_actual_ " + 
										//	",item_point_max_ " + 
										"from  " + 
											"item_grade_ " + 
											",select_category_item_components " + 
											",get_id_student_ " + 
										"where " + 
											"item_grade_.id_student_  = get_id_student_.id_student_ " + 
											"AND  " + 
												"( " + 
													// TH: receives output data from earlier subquery.
													"id_item_ = item_id_ " + 
												") " + 
										"group by " + 
											"id_category_ " + 
											",weight_category_ " + 
											",get_id_student_.id_student_ " + 
											",id_class___ " + 
										//	",item_id_ " + 
										//	",item_name_ " + 
										//	",name_category_ " + 
										//	",point_actual_ " + 
										//	",item_point_max_ " + 
										//	",item_description_ " + 
										//"order by " + 
										//	"point_actual_ asc " + 
				//
						") " + 
					"select " + 
						"id_student_ " + 
						",id_category_ " + 
						",id_class___ " + 
						",cast(count_name_category_ as double precision) * weight_category_ as denominator_component_per_category_ " + 
					"from " + 
						"get_denominator_components " + 
		//
			")module_A8_ " + 
			"using(id_student_,id_category_,id_class___) " + 
//
	") numeric_grade_sub_ " + 
"; "
;
        ;
        
        int id_student_  = 0;
        int id_class___  = 0;
        int id_category_ = 0;
        double grade_per_category_ = 0;
        String letter_grade_per_category_ = null;
        try (PreparedStatement stmt = db.prepareStatement(grade_letter_per_student_per_category_)) {
           
           stmt.setString(1,username);
           stmt.setInt(2,activeClass_id);
           stmt.setString(3,username);
           stmt.setInt(4,activeClass_id);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              
              System.out.println("STUDENT ID\tCLASS ID\tCATEGORY ID\tNUMERIC GRADE\tLETTER GRADE");
              do {
                 
                 id_student_  = rs.getInt("id_student_");
                 id_class___  = rs.getInt("id_class___");
                 id_category_ = rs.getInt("id_category_");
                 grade_per_category_ = rs.getDouble("grade_per_category_");
                 letter_grade_per_category_ = rs.getString("letter_grade_per_category_");
                 
                 System.out.print(id_student_  + "\t\t");
                 System.out.print(id_class___  + "\t\t");
                 System.out.print(id_category_ + "\t\t");
                 System.out.format("%1.5f\t\t", grade_per_category_);
                 System.out.println(letter_grade_per_category_);
                 
              } while(rs.next());
              
           }
           
        }
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        // STEP 5b: class.
        
        String grade_letter_per_student_per_class_ = 
// TH: retrieves letter grade for given student in given class.
// TH: retrieves letter grade for given student in given class.
"select " + 
	"id_student_ " + 
	",id_class___ " + 
	",numeric_grade_per_class_ " + 
	",case " + 
		"when numeric_grade_per_class_ >= 0.96 then 'A+' " + 
		"when numeric_grade_per_class_ >= 0.93 AND numeric_grade_per_class_ < 0.96 then 'A' " + 
		"when numeric_grade_per_class_ >= 0.90 AND numeric_grade_per_class_ < 0.93 then 'A-' " + 
		"when numeric_grade_per_class_ >= 0.86 AND numeric_grade_per_class_ < 0.90 then 'B+' " + 
		"when numeric_grade_per_class_ >= 0.83 AND numeric_grade_per_class_ < 0.86 then 'B' " + 
		"when numeric_grade_per_class_ >= 0.80 AND numeric_grade_per_class_ < 0.83 then 'B-' " + 
		"when numeric_grade_per_class_ >= 0.76 AND numeric_grade_per_class_ < 0.80 then 'C+' " + 
		"when numeric_grade_per_class_ >= 0.73 AND numeric_grade_per_class_ < 0.76 then 'C' " + 
		"when numeric_grade_per_class_ >= 0.70 AND numeric_grade_per_class_ < 0.73 then 'C-' " + 
		"when numeric_grade_per_class_ >= 0.66 AND numeric_grade_per_class_ < 0.70 then 'D+' " + 
		"when numeric_grade_per_class_ >= 0.63 AND numeric_grade_per_class_ < 0.66 then 'D' " + 
		"when numeric_grade_per_class_ >= 0.60 AND numeric_grade_per_class_ < 0.63 then 'D-' " + 
		"when numeric_grade_per_class_ >= 0.56 AND numeric_grade_per_class_ < 0.60 then 'E+' " + 
		"when numeric_grade_per_class_ >= 0.53 AND numeric_grade_per_class_ < 0.56 then 'E' " + 
		"when numeric_grade_per_class_ >= 0.50 AND numeric_grade_per_class_ < 0.53 then 'E-' " + 
		"when numeric_grade_per_class_ >= 0.46 AND numeric_grade_per_class_ < 0.50 then 'F+' " + 
		"when numeric_grade_per_class_ >= 0.43 AND numeric_grade_per_class_ < 0.46 then 'F' " + 
		"when numeric_grade_per_class_ >= 0.40 AND numeric_grade_per_class_ < 0.43 then 'F-' " + 
		"when numeric_grade_per_class_ >= 0.36 AND numeric_grade_per_class_ < 0.40 then 'G+' " + 
		"when numeric_grade_per_class_ >= 0.33 AND numeric_grade_per_class_ < 0.36 then 'G' " + 
		"when numeric_grade_per_class_ >= 0.30 AND numeric_grade_per_class_ < 0.33 then 'G-' " + 
		"when numeric_grade_per_class_ >= 0.26 AND numeric_grade_per_class_ < 0.30 then 'H+' " + 
		"when numeric_grade_per_class_ >= 0.23 AND numeric_grade_per_class_ < 0.26 then 'H' " + 
		"when numeric_grade_per_class_ >= 0.20 AND numeric_grade_per_class_ < 0.23 then 'H-' " + 
		"when numeric_grade_per_class_ >= 0.16 AND numeric_grade_per_class_ < 0.20 then 'I+' " + 
		"when numeric_grade_per_class_ >= 0.13 AND numeric_grade_per_class_ < 0.16 then 'I' " + 
		"when numeric_grade_per_class_ >= 0.10 AND numeric_grade_per_class_ < 0.13 then 'I-' " + 
		"when numeric_grade_per_class_ >= 0.06 AND numeric_grade_per_class_ < 0.10 then 'J+' " + 
		"when numeric_grade_per_class_ >= 0.03 AND numeric_grade_per_class_ < 0.06 then 'J' " + 
		"when numeric_grade_per_class_ <  0.03 then 'J-' " + 
	"end as letter_grade_per_class_ " + 
"from " + 
	"( " + 
//
		// TH: retrieves numeric grade for given student in given class.
		"select " + 
			"* " + 
			",numerator_component_sum_per_class_ / denominator_component_sum_per_class_ as numeric_grade_per_class_ " + 
		"from " + 
			"( " + 
		//
				// TH: retrieves total numerator value and total denominator value from all category instances, 
				// TH: which helps calculate overal grade for given student in given class.
				//// TH: calculates numeric grade per category for given student in given class.
				//// TH: A7+A8.
				//// TH: gets numerator and denominator components for grade calculation for given student in given class.
				"select " + 
					"id_student_ " + 
					",id_class___ " + 
					",sum(numerator_component_sum_per_category_) as numerator_component_sum_per_class_ " + 
					",sum(denominator_component_per_category_) as denominator_component_sum_per_class_ " + 
				"from " + 
					"( " + 
						//
						// TH: gets subtotal per category, from all items for given student in given class.
						// TH: module A7.
						//// TH: gets numerator components from all items for given student in given class, grouped by category.
						//// TH: see debug_0_part2_.png for numerator sample data.
						//// TH: see debug_A6_.png for denominator sample data.
						"with " + 
							"get_numerator_components as " + 
								"( " + 
						//
									// IMPORTANT: lists records for all items for given student in given class, grouped by category.
									// TH: module A2+A3 -> A5.
									// TH: CTE version.

											// TH: retrieves all categories from all classes enrolled by given student.
											// TH: retrieves all items for given (recently retrieved) category IDs.
											// TH: module A0+A1 -> A2.
											// TH: retrieves ONLY row(s) for given class (id_: 330) -> item_.id_: 600, 185, 520, 736, 34.
											"with " + 
												"get_id_student_ as " + 
													"( " + 
														"select " + 
															"student_.id_ as id_student_ " + 
														"from " + 
															"student_ " + 
														"where " + 
															"student_.username_ = ? " + 
													") " + 
												",get_id_class_ as  " + 
													"( " + 
														"select " + 
															"id_class_ as id_class__ " + 
														"from " + 
															"enrollment_ " + 
														"where " + 
															"id_student_ in " + 
															"( " + 
																"select * from get_id_student_ " + 
															") " + 
													") " + 
												",select_category_ as " + 
													"( " + 
														"select " + 
															"id_class__ as id_class___ " + 
															",category_.id_ as id_category__ " + 
															",category_.name_ as name_category_ " + 
															",category_.weight_ as weight_category_ " + 
															",category_.id_class_ as class_category_ " + 
														"from " + 
															"category_ " + 
															",get_id_class_ " + 
														"where " + 
															"category_.id_class_ = id_class__ " + 
															"AND id_class__ = ? " + 
													") " + 
												",select_category_item_components as " + 
													"( " + 
														"select  " + 
															"id_class___ " + 
															",item_.id_ as item_id_ " + 
															",item_.name_ as item_name_ " + 
															",item_.point_max_ as item_point_max_ " + 
															",item_.id_category_ as item_id_category_ " + 
															",item_.description_ as item_description_ " + 
															",name_category_ " + 
															",weight_category_ " + 
														"from " + 
															"item_ " + 
															",select_category_ " + 
														"where " + 
															"id_category_ = id_category__ " + 
													") " + 
												// TH: retrieves actual point per item (i.e. by ID) for given student.
												// TH: module A3.
												"select " + 
													"get_id_student_.id_student_ as id_student_ " + 
													",id_class___ " + 
													",item_id_category_ as id_category_ " + 
													",name_category_ " + 
													",weight_category_ " + 
													",item_id_ " + 
													",item_name_ " + 
													",item_description_ " + 
													",point_actual_ " + 
													",item_point_max_ " + 
												"from  " + 
													"item_grade_ " + 
													",select_category_item_components " + 
													",get_id_student_ " + 
												"where " + 
													"item_grade_.id_student_  = get_id_student_.id_student_ " + 
													"AND  " + 
														"( " + 
															// TH: receives output data from earlier subquery.
															"id_item_ = item_id_ " + 
														") " + 
												"group by " + 
													"id_category_ " + 
													",weight_category_ " + 
													",get_id_student_.id_student_ " + 
													",id_class___ " + 
													",item_id_ " + 
													",item_name_ " + 
													",name_category_ " + 
													",point_actual_ " + 
													",item_point_max_ " + 
													",item_description_ " + 
												"order by " + 
													"point_actual_ asc " + 
						//
								") " + 
							"select  " + 
								"id_student_ " + 
								",id_category_ " + 
								",id_class___ " + 
								",sum(point_actual_ / item_point_max_ * weight_category_) as numerator_component_sum_per_category_ " + 
							"from " + 
								"get_numerator_components " + 
							"group by " + 
								"id_category_ " + 
								",id_student_ " + 
								",id_class___ " + 
				//
					")module_A7_ " + 
					"inner join " + 
					"( " + 
				//
						// TH: gets denominator components from all category instances for given student in given class.
						// TH: module A8.
						// TH: see debug_A6_.png for denominator sample data.
						// TH: see debug_0_part2_.png for numerator sample data.
						"with " + 
							"get_denominator_components as " + 
								"( " + 
						//
									// TH: retrieves count of instances per category along with its weight,
									// TH: from given class enrolled by given student.
									// TH: module A6 -> represents denominator in grade calculation per student per class.
									//// IMPORTANT: lists records for all items for given student in given class, grouped by category.
									//// TH: module A2+A3 -> A5.
									//// TH: CTE version.

											// TH: retrieves all categories from all classes enrolled by given student.
											// TH: retrieves all items for given (recently retrieved) category IDs.
											// TH: module A0+A1 -> A2.
											// TH: retrieves ONLY row(s) for given class (id_: 330) -> item_.id_: 600, 185, 520, 736, 34.
											"with " + 
												"get_id_student_ as " + 
													"( " + 
														"select " + 
															"student_.id_ as id_student_ " + 
														"from " + 
															"student_ " + 
														"where " + 
															"student_.username_ = ? " + 
													") " + 
												",get_id_class_ as  " + 
													"( " + 
														"select " + 
															"id_class_ as id_class__ " + 
														"from " + 
															"enrollment_ " + 
														"where " + 
															"id_student_ in " + 
															"( " + 
																"select * from get_id_student_ " + 
															") " + 
													") " + 
												",select_category_ as " + 
													"( " + 
														"select " + 
															"id_class__ as id_class___ " + 
															",category_.id_ as id_category__ " + 
															",category_.name_ as name_category_ " + 
															",category_.weight_ as weight_category_ " + 
															",category_.id_class_ as class_category_ " + 
														"from " + 
															"category_ " + 
															",get_id_class_ " + 
														"where " + 
															"category_.id_class_ = id_class__ " + 
															"AND id_class__ = ? " + 
													") " + 
												",select_category_item_components as " + 
													"( " + 
														"select  " + 
															"id_class___ " + 
															",item_.id_ as item_id_ " + 
															",item_.name_ as item_name_ " + 
															",item_.point_max_ as item_point_max_ " + 
															",item_.id_category_ as item_id_category_ " + 
															",item_.description_ as item_description_ " + 
															",name_category_ " + 
															",weight_category_ " + 
														"from " + 
															"item_ " + 
															",select_category_ " + 
														"where " + 
															"id_category_ = id_category__ " + 
													") " + 
												// TH: retrieves count of instances per category along with its weight,
												// TH: from given class enrolled by given student.
												"select " + 
													"item_id_category_ as id_category_ " + 
													",count(name_category_) as count_name_category_ " + 
													",weight_category_ " + 
													",get_id_student_.id_student_ as id_student_ " + 
													",id_class___ " + 
												//	",item_id_category_ as id_category_ " + 
												//	",item_id_ " + 
												//	",item_name_ " + 
												//	",item_description_ " + 
												//	",point_actual_ " + 
												//	",item_point_max_ " + 
												"from  " + 
													"item_grade_ " + 
													",select_category_item_components " + 
													",get_id_student_ " + 
												"where " + 
													"item_grade_.id_student_  = get_id_student_.id_student_ " + 
													"AND  " + 
														"( " + 
															// TH: receives output data from earlier subquery.
															"id_item_ = item_id_ " + 
														") " + 
												"group by " + 
													"id_category_ " + 
													",weight_category_ " + 
													",get_id_student_.id_student_ " + 
													",id_class___ " + 
												//	",item_id_ " + 
												//	",item_name_ " + 
												//	",name_category_ " + 
												//	",point_actual_ " + 
												//	",item_point_max_ " + 
												//	",item_description_ " + 
												//"order by " + 
												//	"point_actual_ asc " + 
						//
								") " + 
							"select " + 
								"id_student_ " + 
								",id_category_ " + 
								",id_class___ " + 
								",cast(count_name_category_ as double precision) * weight_category_ as denominator_component_per_category_ " + 
							"from " + 
								"get_denominator_components " + 
				//
					")module_A8_ " + 
					"using(id_student_,id_category_,id_class___) " + 
				"group by " + 
					"id_student_ " + 
					",id_class___ " + 
		//
			") component_per_class_sub_ " + 
//
	") numeric_grade_per_class_ " + 
"; "
;
        ;
        
        id_student_  = 0;
        id_class___  = 0;
        double numeric_grade_per_class_ = 0;
        String letter_grade_per_class_ = null;
        try (PreparedStatement stmt = db.prepareStatement(grade_letter_per_student_per_class_)) {
           
           stmt.setString(1,username);
           stmt.setInt(2,activeClass_id);
           stmt.setString(3,username);
           stmt.setInt(4,activeClass_id);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              
              System.out.println("STUDENT ID\tCLASS ID\tNUMERIC GRADE\tLETTER GRADE (TOTAL)");
              do {
                 
                 id_student_  = rs.getInt("id_student_");
                 id_class___  = rs.getInt("id_class___");
                 numeric_grade_per_class_ = rs.getDouble("numeric_grade_per_class_");
                 letter_grade_per_class_  = rs.getString("letter_grade_per_class_");
                 
                 System.out.print(id_student_  + "\t\t");
                 System.out.print(id_class___  + "\t\t");
                 System.out.format("%1.5f\t\t", numeric_grade_per_class_);
                 System.out.println(letter_grade_per_class_);
                 
              } while(rs.next());
              
           }
           
        }
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        return "Success";
    }

    @Command(description="Shows gradebook for currently selected class with default grade assigned (if applicable).")
    public String gradebook(){
        // show the current class’s gradebook:
        // students (username, student ID, and name),
        // along with their total grades in the class.
        
        if(!isClassSelected()){
            return noClassString;
        }
        
        String query_student_per_class_ =
        // TH: retrieves student information in given class.
        "select " + 
           "id_class_ as id_class__ " + 
           ",id_ as id_student__ " + 
           ",username_ as username__ " + 
           ",realname_ as realname__ " + 
        "from  " + 
           "student_ " + 
           ",enrollment_ " + 
        "where " + 
           "id_ = id_student_ " + 
           "AND id_class_ = ? " + 
        "order by " + 
           "id_student__ " + 
        "; "
        ;
        
        // TH: holds student's information in given class.
//      List<String> username__ = new ArrayList<String> ();
//      List<String> realname__ = new ArrayList<String> ();
        
        try (PreparedStatement stmt = db.prepareStatement(query_student_per_class_)) {
           
           stmt.setInt(1,activeClass_id);
           
           try (ResultSet rs = stmt.executeQuery()) {
              
              if (!rs.next()) { /* TH: hopefully never happens :( */ }
              
              do {
                 
//               username__.add(rs.getString("username__"));
//               realname__.add(rs.getString("realname__"));
                 
                 System.out.println("----------------------------------------------------------------------------");
                 System.out.println("[studentGrades]: id_student__                            == " + rs.getInt("id_student__") + ".");
                 System.out.println("[studentGrades]: realname                                == " + rs.getString("realname__") + ".");
                 studentGrades(rs.getString("username__"));
//               System.out.println();
                 
              } while(rs.next());
              
              System.out.println("----------------------------------------------------------------------------");
              
           }
           
        }
        catch (SQLException sqEx){
            return "SQL ERROR: " + sqEx;
        }
        
        return "Success.";
    }

    // TH: references https://www.tutorialspoint.com/how-to-read-the-data-from-a-csv-file-in-java
    @Command(description="Imports grades for students on given item from CSV file.")
    public String importGrades(String itemname, String csvFile){
       
       if(!isClassSelected()){
            return noClassString;
       }
       
       String delimiter = ",";
       
       File file = null;
       try{
          file = new File("/home/tungho/Downloads/CS410-Project/" + csvFile);
       }//try.
       catch(NullPointerException e){
          System.out.println("[importGrades]: Ended (NullPointerException).");
          System.exit(1);
       }//catch NullPointerException.
       
       FileReader fr =  null;
       try{
          fr = new FileReader(file);
       }//try.
       catch(FileNotFoundException e){
          e.printStackTrace();
          System.out.println("[importGrades]: Ended (FileNotFoundException).");
          System.exit(1);
       }//catch FileNotFoundException.
       
       BufferedReader br = new BufferedReader(fr);
       
       // TH: gets allocation from callee.
       String line = null;
       String[] tempArr = null;
       
       try{
          
          while((line = br.readLine()) != null) {
             
             try{
                tempArr = line.split(delimiter);
             }//try.
             catch (PatternSyntaxException e){
                System.out.println("[importGrades]: Ended (PatternSyntaxException).");
                System.exit(1);
             }//catch PatternSyntaxException.
             
             /*
             for(String tempStr : tempArr) {
                System.out.print(tempStr + " ");
             }
             System.out.println();
             */
             
             // TH: assigns a given grade to given student on given item.
//           grade(itemname, tempArr[0], Double.parseDouble(tempArr[1]));
             
             if( grade(itemname, tempArr[0], Double.parseDouble(tempArr[1])).startsWith("ERROR") == true ) {
                System.out.println("[importGrades]: ERROR --- " + tempArr[0] + ", " + itemname + ".");
             }//if true.
             
          }//while !null.
          
          br.close();
          
       }//try.
       catch(IOException e){
//        System.out.println(e.printStackTrace());
          System.out.println("[importGrades]: Ended (IOException).");
          System.exit(1);
       }//catch IOException.
       
       return "Success";
       
    }//importGrades.

    @Command(description="Deselects the active class.")
    public String deselectClass(){
        String retString;
        if(isClassSelected()){
            activeClass = null;
            activeClass_id = -1;
            retString = " - > succesfully deselected class\n";
        } else {
            retString = noClassString;
        }
        return retString;
    }

    /**
     * private function to check if a class is currently selected
     * @param name
     * @param weight
     * @return
     */
    private boolean isClassSelected() {
        if (activeClass != null) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        
        // TH: gets postgresql JDBC driver.
        try{
           Class.forName("org.postgresql.Driver");
        }//try.
        catch(ExceptionInInitializerError e){
           System.out.println("ERROR: " + e.toString());
           return;
        }//catch ExceptionInInitializerError.
        catch(LinkageError e){
           System.out.println("ERROR: " + e.toString());
           return;
        }//catch LinkageError.
        catch(ClassNotFoundException e){
           System.out.println("ERROR: " + e.toString());
           return;
        }//catch ClassNotFoundException.
        
        // First (and only) command line argument: database URL
        String dbUrl = args[0];
        try (Connection cxn = DriverManager.getConnection("jdbc:" + dbUrl)) {
            GradeBookShell shell = new GradeBookShell(cxn);
            System.out.println("[main]: Cliche Shell Started.");
            ShellFactory.createConsoleShell("gradebook", "", shell).commandLoop();
            System.out.println("[main]: Cliche Shell Ended.");
        } catch(SQLException sqlEx){
            System.out.println("ERROR: " + sqlEx.toString());
            return;
        }

    }
}
