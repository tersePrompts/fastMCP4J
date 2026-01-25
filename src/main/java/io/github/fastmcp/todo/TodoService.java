package io.github.fastmcp.todo;

import java.util.List;

public interface TodoService {
    String add(String title, String description);
    List<Todo> list();
    Todo get(String id);
    Todo update(String id, String title, String description);
    boolean delete(String id);
    boolean markComplete(String id);
    boolean markIncomplete(String id);
    List<Todo> search(String query);
}
