package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import model.RentalDTO;
import model.RentalHistoryDTO;
import oracle.jdbc.OracleTypes;
import util.DBUtil;

public class RentalDAO {
    // 대여내역 가져오기 (SP_GET_RENTAL_HISTORY가 RENTAL 테이블을 사용하고 OVERDUE_FEE 등을 정확히 가져와야 함)
    public List<RentalHistoryDTO> findRentalsByUserId(String userId) {
        List<RentalHistoryDTO> list = new ArrayList<>();
        System.out.println("testtest");
        String sql = "{call SP_GET_RENTAL_HISTORY(?, ?)}"; 

        try (Connection conn = DBUtil.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setString(1, userId);
            cstmt.registerOutParameter(2, OracleTypes.CURSOR);
            cstmt.execute();

            try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                while (rs.next()) {
                    RentalHistoryDTO dto = new RentalHistoryDTO();
                    dto.setRentalNum(rs.getLong("RENTAL_NUM"));
                    // SerialNum도 DTO에 추가하여 사용하면 유용합니다. (alert 메시지 등에)
                    // dto.setSerialNum(rs.getString("SERIAL_NUM")); 
                    dto.setOfficeName(rs.getString("OFFICE_NAME"));
                    dto.setEqName(rs.getString("EQ_NAME"));
                    dto.setRentalDate(rs.getTimestamp("RENTAL_DATE").toLocalDateTime());
                    
                    Timestamp actualReturnTS = rs.getTimestamp("ACTUAL_RETURN_DATE");
                    if (actualReturnTS != null) {
                        dto.setActualReturnDate(actualReturnTS.toLocalDateTime());
                    }
                    dto.setReturnStatus(rs.getString("RETURN_STATUS"));
                    dto.setOverdueDays(rs.getLong("OVERDUE_DAYS")); 
                    dto.setOverdueFee(rs.getLong("OVERDUE_FEE"));   
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * 장비를 반납 처리만 하는 프로시저 호출 (질문자님이 주신 SP_PROCESS_RETURN 사용).
     * @param rentalNum 반납할 대여 번호
     * @return 프로시저에서 반환하는 결과 상태 문자열 (SUCCESS, NOT_FOUND, ALREADY_RETURNED, ERROR)
     */
    public String processReturn(Long rentalNum) { 
        String status = "ERROR"; 
        String sql = "{call SP_PROCESS_RETURN(?, ?)}"; // 파라미터 2개: p_rental_num, o_status

        try (Connection conn = DBUtil.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setLong(1, rentalNum);
            cstmt.registerOutParameter(2, OracleTypes.VARCHAR); 
            cstmt.execute();

            status = cstmt.getString(2); 

        } catch (SQLException e) {
            e.printStackTrace();
            status = "ERROR: " + e.getMessage();
        }
        return status;
    }

    /**
     * 연체료를 0으로 업데이트하는 프로시저 호출 (결제 완료 시 사용).
     * @param rentalNum 연체료를 0으로 업데이트할 대여 번호
     * @return 성공 여부 (SUCCESS, FAIL_NOT_FOUND, FAIL_UNKNOWN)
     */
    public String updateOverdueFeeToZero(Long rentalNum) {
        String status = "FAIL_UNKNOWN";
        String sql = "{call SP_UPDATE_OVERDUE_FEE_TO_ZERO(?, ?)}"; // 제가 제시한 프로시저 사용

        try (Connection conn = DBUtil.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setLong(1, rentalNum);
            cstmt.registerOutParameter(2, OracleTypes.VARCHAR);
            cstmt.execute();

            status = cstmt.getString(2);

        } catch (SQLException e) {
            e.printStackTrace();
            status = "ERROR: " + e.getMessage();
        }
        return status;
    }
    
    public boolean insertRental(RentalDTO rental) {
        String sql = "{ call sp_insert_rental(?, ?, ?, ?) }";

        try (Connection conn = DBUtil.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setLong(1, rental.getUserId());
            cs.setString(2, rental.getSerialNum());
            cs.setTimestamp(3, Timestamp.valueOf(rental.getRentalDate()));
            cs.setTimestamp(4, Timestamp.valueOf(rental.getReturnDate()));

            cs.execute();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}