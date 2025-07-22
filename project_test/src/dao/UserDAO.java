package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import model.UserDTO;
import static util.DBUtil.getConnection;
import static util.Session.*;

public class UserDAO {
	public void addUser(UserDTO user) {
		// try-with-resources를 사용하여 Connection과 CallableStatement를 자동으로 닫도록 변경
		try (Connection conn = getConnection(); // Connection을 try-with-resources에 포함
				CallableStatement cstmt = conn.prepareCall("{call SP_ADD_USER(?, ?, ?, ?, ?)}")) { // CallableStatement도
																									// 포함

			// SQL 쿼리 문자열은 prepareCall 메서드 내부로 바로 전달 가능
			// String sql = "{call SP_ADD_USER(?, ?, ?, ?, ?)}"; // 이 줄은 이제 필요 없습니다.

			cstmt.setString(1, user.getLoginId());
			cstmt.setString(2, user.getPassword());
			cstmt.setString(3, user.getUserName());
			cstmt.setString(4, user.getPhoneNumber());
			cstmt.setString(5, user.getUserGu());

			cstmt.execute();
			System.out.println("사용자 등록 성공");

		} catch (SQLException e) {
			System.err.println("사용자 등록 실패: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean isIdDuplicated(String loginId) {
		boolean result = false;
		String sql = "SELECT COUNT(*) FROM USERS WHERE LOGIN_ID = ?";
		try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, loginId);
			try (ResultSet rs = pstmt.executeQuery();) {
				while (rs.next()) {
					int count = rs.getInt(1); // 첫 번째 컬럼(COUNT(*))의 값을 가져옴
					if (count > 0) { // 개수가 0보다 크면 중복
						result = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean isLoginValid(Connection conn, String loginId, String password) {
		boolean isValid = false;
		String sql = "SELECT * FROM USERS WHERE LOGIN_ID = ? AND PASSWORD = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // pstmt만 먼저 생성
			pstmt.setString(1, loginId); // 첫 번째 바인딩 변수 설정
			pstmt.setString(2, password); // 두 번째 바인딩 변수 설정

			try (ResultSet rs = pstmt.executeQuery()) { // 바인딩 변수 설정 후 쿼리 실행
				if (rs.next()) {
					isValid = true;
					userPassword = rs.getString("PASSWORD");
					userName = rs.getString("USER_NAME");
					userPhoneNumber = rs.getString("PHONE_NUMBER");
					userGu = rs.getString("USER_GU");
				}
			} // rs가 이 블록을 벗어나면 자동으로 close() 됨
		} catch (Exception e) { // SQLException으로 특정하는 것이 더 좋음
			e.printStackTrace();
		}
		return isValid;
	}

	public String updateUserInfo(String loginId, String oldPassword, String newName, String newPhoneNumber,
			String newGu, String newPassword) {
		String resultStatus = "UNKNOWN_ERROR";

		// try-with-resources를 사용하여 Connection과 CallableStatement를 자동으로 닫도록 변경
		try (Connection conn = getConnection(); // Connection을 try-with-resources에 포함
				CallableStatement cstmt = conn.prepareCall("{call SP_UPDATE_USER_INFO(?, ?, ?, ?, ?, ?, ?)}")) {

			// IN 파라미터 설정
			cstmt.setString(1, loginId);
			cstmt.setString(2, oldPassword);
			cstmt.setString(3, newName);
			cstmt.setString(4, newPhoneNumber);
			cstmt.setString(5, newGu);
			cstmt.setString(6, (newPassword != null && !newPassword.isEmpty()) ? newPassword : null);

			// OUT 파라미터 등록
			cstmt.registerOutParameter(7, Types.VARCHAR);

			// 프로시저 실행
			cstmt.execute();

			// OUT 파라미터 값 가져오기
			resultStatus = cstmt.getString(7);

		} catch (SQLException e) { // 데이터베이스 관련 예외는 SQLException으로 특정하는 것이 좋습니다.
			System.err.println("데이터베이스 오류 발생: " + e.getMessage());
			e.printStackTrace(); // 스택 트레이스는 디버깅에 매우 중요합니다.
			resultStatus = "ERROR: " + e.getMessage(); // 오류 메시지를 반환하여 UI에 표시 가능
		} catch (Exception e) { // 그 외 예상치 못한 모든 예외 처리
			System.err.println("일반 오류 발생: " + e.getMessage());
			e.printStackTrace();
			resultStatus = "ERROR: " + e.getMessage(); // 오류 메시지를 반환
		}

		return resultStatus;
	}

	// 사용자 ID 조회
	public Long getUserIdByLoginID(String loginId) {
		String sql = "{ call sp_get_user_id_by_login_id(?, ?) }";
		try (Connection conn = getConnection(); CallableStatement cs = conn.prepareCall(sql)) {

			cs.setString(1, loginId);
			cs.registerOutParameter(2, Types.NUMERIC);

			cs.execute();
			return cs.getLong(2);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
