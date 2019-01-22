package web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.util.Objects;

@Entity
@SelectBeforeUpdate(value = false)
public class Task implements Persistable<Long> {

    private static final long serialVersionUID = 1L;

    public Task() {
    }

    public Task(long id, String payload, String info, Integer status) {
        super();
        this.id = id;
        this.payload = payload;
        this.info = info;
        this.status = status;
    }
    public Task(String payload, String info, Integer status) {
        super();
        this.payload = payload;
        this.info = info;
        this.status = status;
    }

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name="pk_sequence",sequenceName="task_sequence", allocationSize = 10)
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="pk_sequence")
    private long id;
    @Column(nullable = false)
    private String payload;
    @Column(nullable = false)
    private String info;
    @Column(nullable = false)
    private Integer status;

    public Long getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return id == 0;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLink() {
        return payload;
    }

    public void setLink(String payload) {
        this.payload = payload;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
            Objects.equals(payload, task.payload) &&
            Objects.equals(info, task.info) &&
            Objects.equals(status, task.status);
    }
}
