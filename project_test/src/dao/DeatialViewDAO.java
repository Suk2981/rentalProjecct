package dao;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;

import model.DetailViewDTO;
import util.DBUtil;

public class DeatialViewDAO {
	public DetailViewDTO getEachEquipmentDetail(String serialNum) {
		DetailViewDTO dto = new DetailViewDTO();
	    String sql = "{ call sp_get_each_equipment_info(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

	    try (Connection conn = DBUtil.getConnection();
	         CallableStatement cs = conn.prepareCall(sql)) {

	        cs.setString(1, serialNum);
	        cs.registerOutParameter(2, Types.VARCHAR); // eq_name
	        cs.registerOutParameter(3, Types.VARCHAR); // state
	        cs.registerOutParameter(4, Types.DATE);    // get_date
	        cs.registerOutParameter(5, Types.CLOB);    // eq_info
	        cs.registerOutParameter(6, Types.NUMERIC); // fee
	        cs.registerOutParameter(7, Types.VARCHAR); // office
	        cs.registerOutParameter(8, Types.VARCHAR); // phone
	        cs.registerOutParameter(9, Types.VARCHAR); // img_url

	        cs.execute();

	        dto.setSerialNum(serialNum);
	        dto.setEqName(cs.getString(2));
	        dto.setState(cs.getString(3));
	        dto.setGetDate(cs.getDate(4));
	        Clob clob = cs.getClob(5);
	        if (clob != null) {
	            dto.setEqInfo(clob.getSubString(1, (int) clob.length()));
	        }
	        dto.setFee(cs.getInt(6));
	        dto.setRentalOffice(cs.getString(7));
	        dto.setPhone(cs.getString(8));
	        dto.setImgPath(cs.getString(9));
	       
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return dto;
	}
	
}
