package web.dao;

import web.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, QueryDslPredicateExecutor<Task> {


}
