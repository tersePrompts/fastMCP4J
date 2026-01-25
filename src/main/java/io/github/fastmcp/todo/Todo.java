package io.github.fastmcp.todo;

import lombok.Value;

@Value
public class Todo {
    String id;
    String title;
    String description;
    boolean completed;
    long createdAt;
    long updatedAt;
    
    public static Todo of(String id, String title, String description, boolean completed) {
        long now = System.currentTimeMillis();
        return new Todo(id, title, description, completed, now, now);
    }
}
