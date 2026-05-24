package br.com.ada.quarkus.resource;

import java.util.List;
import br.com.ada.quarkus.util.PageResult;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {

        if (result == null) {
            throw new IllegalArgumentException("PageResult não pode ser null");
        }

        List<R> mapped = result.content()
                .stream()
                .map(mapper)
                .toList();

        return new PageResponse<>(
                mapped,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}