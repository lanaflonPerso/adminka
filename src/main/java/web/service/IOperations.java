package web.service;

import org.springframework.data.domain.Page;

import java.util.List;

public interface IOperations<T> {

    public Page<T> findPaginated(final int page, final int size, final String sortField, final String sortDirection);

    public T insert(T object);
    public List<T> insert(List<T> objects);

}
