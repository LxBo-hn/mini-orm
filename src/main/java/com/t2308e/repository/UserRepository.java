package com.t2308e.repository;

import com.t2308e.entity.User;

// Không cần thêm annotation gì ở đây
public interface UserRepository extends MyCrudRepository<User, Long> {
    // Có thể định nghĩa thêm các phương thức truy vấn tùy chỉnh ở đây (ngoài phạm vi bài tập này)
    // Ví dụ: Optional<User> findByUserName(String userName);
    // Việc implement các phương thức này sẽ phức tạp hơn, cần parse tên phương thức để sinh SQL.
}