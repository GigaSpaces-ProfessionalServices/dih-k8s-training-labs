package com.gs;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.jdbc.GSConnection;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.tomcat.util.http.parser.MediaType;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


@RequestMapping()
@RestController
public class DataController {
    @Value("${space.name}")
    private String spaceName;

    @Value("${space.group}")
    private String group;

    @Value("${space.manager}")
    private String locators;

    @Value("${user}")
    private String user;

    @Value("${password}")
    private String password;

    @Value("${query.limit}")
    private int limit;


    private GigaSpace space;

    private Connection connection;
    @ApiOperation(value = "Welcome")
    @GetMapping("/")
    public ResponseEntity<String> welcome(){
        return new ResponseEntity<>("WELCOME to space rest api example! working with space:"  + spaceName, HttpStatus.OK);
    }

    @ApiOperation(value="Query table",notes="Returns first rows of the table")
    @GetMapping("/queryrs")
    public ResponseEntity<String> query(@RequestParam String tableName) throws Exception {
        System.out.println("============ Query was called:" + spaceName);
        space = connectToSpace();
        if (!tableExists(tableName)) return new ResponseEntity<>("Failed to run select Table "+ tableName + " Doesn't exists in space " + spaceName, HttpStatus.BAD_REQUEST);
        ResultSet rs = executeQuery("select * from " + "\"" + tableName +"\" LIMIT "+limit ) ;
        return new ResponseEntity<>(dumpResult(rs), HttpStatus.OK);
    }

    public boolean tableExists(String tableName){
        SpaceTypeDescriptor typeDescriptor = space.getTypeManager().getTypeDescriptor(tableName);
        if (typeDescriptor == null)   return false; else return true;
    }

    @ApiOperation(value="Insert new entry to table",notes="Will override entry if same id already exists")
    @PostMapping("/insert")
    public ResponseEntity<String> newEntry(@RequestParam String tableName, @RequestBody Map<String,Object> properties) throws Exception {
        space = connectToSpace();
        if (!tableExists(tableName))
            return new ResponseEntity<>("Failed to insert object Type:" + tableName + "doesn't exists in space:" + spaceName, HttpStatus.BAD_REQUEST);
        SpaceDocument spaceDocument = new SpaceDocument(tableName);
        spaceDocument.addProperties(properties);
        try {
            space.write(spaceDocument);
        }
        catch (Throwable t){
            new ResponseEntity<>( "Failed to insert object: " + t, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("Object inserted successfully", HttpStatus.OK);
    }


    @ApiOperation(value="Create a new table in space using create table query",notes="Will Fail if table already exists")
    @PostMapping("/newtable")
    public ResponseEntity<String> createTable(@RequestBody String query) throws Exception {
        connectToSpace();
        String tableName = findTableName(query);
        if (tableExists(tableName)) {
            return new ResponseEntity<>("Fail to create table:" + tableName + " table already exists in space " + spaceName, HttpStatus.BAD_REQUEST);
        }
        updateQuery(query);
        return  new ResponseEntity<>("Table was created successfully", HttpStatus.OK);
    }

    protected String findTableName(String query) throws Exception{
        String[] words = query.split(" ");
        String queryLC = query.toLowerCase();
        if (queryLC.startsWith("create"))
            return words[2];
        if (queryLC.startsWith("select")){
            for (int i=0; i < words.length; i++){
                if (words[i].equalsIgnoreCase("from"))
                    return words[i+1];
            }
        }
        throw new Exception("Can't extract table name from query:" + query);
    }

    private boolean updateQuery(String query) throws SQLException {
        space = connectToSpace();
        try  {
            Statement stmt = connection.createStatement();
             stmt.execute(query);
             return true;
        }
        catch (Exception e){
            System.out.println("Fail to run query:" + query);
            throw e;
        }
    }



    private ResultSet executeQuery(String query) throws SQLException {
        space = connectToSpace();
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);

    }


    public String dumpResult(ResultSet resultSet) throws SQLException {
        StringBuilder output = new StringBuilder();
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        for (int k=1; k<= columnsNumber; k++){
            if (k > 1) output.append("\t");
            output.append(rsmd.getColumnName(k));
        }
        output.append("\n\r");
        while (resultSet.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) output.append("|\t");
                String columnValue = resultSet.getString(i);
                output.append(columnValue);
            }
            output.append("\n\r");
        }
        return output.toString();
    }

    private GigaSpace connectToSpace() throws SQLException{
        if (space != null)
            return space;

        space = new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)
                .lookupLocators(locators).lookupGroups(group).credentials(user, password)).create();
        Properties props = new Properties();
        connection = GSConnection.getInstance(space.getSpace(), props);
        return space;
    }


}
