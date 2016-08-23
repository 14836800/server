package com.duoshouji.server.internal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.duoshouji.server.service.dao.BasicUserDto;
import com.duoshouji.server.service.dao.NoteDto;
import com.duoshouji.server.service.dao.RegisteredUserDto;
import com.duoshouji.server.service.dao.UserNoteDao;
import com.duoshouji.server.service.note.NoteFilter;
import com.duoshouji.server.service.note.NotePublishAttributes;
import com.duoshouji.server.util.Image;
import com.duoshouji.server.util.IndexRange;
import com.duoshouji.server.util.MobileNumber;

public class MysqlUserNoteDao implements UserNoteDao {

	private JdbcTemplate mysqlDataSource;

	public MysqlUserNoteDao(JdbcTemplate mysqlDataSource) {
		this.mysqlDataSource = mysqlDataSource;
	}

	@Override
	public RegisteredUserDto findUser(MobileNumber mobileNumber) {
		return queryUser("select * from duoshouji.v_user where mobile = " + mobileNumber);
	}

	@Override
	public RegisteredUserDto findUser(String token) {
		return queryUser("select * from duoshouji.v_user where token = '" + token + "'");
	}
	
	private RegisteredUserDto queryUser(String sql) {
		return mysqlDataSource.query(sql
				, new ResultSetExtractor<RegisteredUserDto>(){
					@Override
					public RegisteredUserDto extractData(ResultSet rs)
						throws SQLException, DataAccessException {
						RegisteredUserDto returnValue = null;
						if (rs.next()) {
							returnValue = new RegisteredUserDto();
							returnValue.mobileNumber = new MobileNumber(Long.toString(rs.getLong("mobile")));
							returnValue.nickname = rs.getString("user_name");
							returnValue.passwordDigest = rs.getString("password");
							returnValue.portrait = new Image(rs.getInt("avatar_width"), rs.getInt("avatar_height"), rs.getString("avatar_url"));
						}
						return returnValue;
				}
		});
	}

	@Override
	public List<NoteDto> findNotes(long cutoff, IndexRange range, NoteFilter filter) {
		StringBuilder sqlBuilder = new StringBuilder("select * from duoshouji.v_square_notes where create_time < " + cutoff);
		if (filter != null) {
			sqlBuilder.append(" and mobile = ");
			sqlBuilder.append(filter.getOwnerId().toString());
		}
		sqlBuilder.append(" order by create_time desc");
		List<NoteDto> returnValue = mysqlDataSource.query(sqlBuilder.toString()
				, new RowMapper<NoteDto>(){
					@Override
					public NoteDto mapRow(ResultSet rs, int rowNum) throws SQLException {
						NoteDto noteDto = new NoteDto();
						noteDto.commentCount = rs.getInt("comment_number");
						noteDto.likeCount = rs.getInt("like_number");
						noteDto.mainImage = new Image(rs.getInt("image1_width"), rs.getInt("image1_height"), rs.getString("image1"));
						noteDto.noteId = rs.getLong("id");
						noteDto.publishedTime = rs.getLong("create_time");
						noteDto.rank = (int)rs.getFloat("rank");
						noteDto.title = rs.getString("title");
						noteDto.transactionCount = rs.getInt("order_number");
						BasicUserDto userDto = new BasicUserDto();
						userDto.mobileNumber = new MobileNumber(Long.toString(rs.getLong("mobile")));
						userDto.nickname = rs.getString("user_name");
						userDto.portrait = new Image(rs.getInt("avatar_width"), rs.getInt("avatar_height"), rs.getString("avatar_url"));
						noteDto.owner = userDto;
						return noteDto;
					}
				});
		if (range != null) {
			returnValue = returnValue.subList(range.getStartIndex(), range.getEndIndex());
		}
		return returnValue;
	}

	@Override
	public void addUser(final MobileNumber mobileNumber) {
		mysqlDataSource.update("insert into duoshouji.user (mobile) values(?)"
				, new PreparedStatementSetter(){
					@Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setLong(1, Long.valueOf(mobileNumber.toString()));
					}
		});
	}

	@Override
	public void removeToken(MobileNumber mobileNumber) {
		mysqlDataSource.update("update duoshouji.user_wechat_login set token = null where user_id in (select id from duoshouji.user where mobile = "+mobileNumber+")");
	}

	@Override
	public void saveToken(final MobileNumber mobileNumber, final String token) {
		mysqlDataSource.update("update duoshouji.user_wechat_login set token = ? where user_id in (select id from duoshouji.user where mobile = ?)"
				,new PreparedStatementSetter(){
					@Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setString(1, token);
						ps.setLong(2, Long.valueOf(mobileNumber.toString()));
					}
				});
	}

	@Override
	public void saveUserProfile(final MobileNumber mobileNumber, final String nickname) {
		mysqlDataSource.update("update duoshouji.user set user_name = ? where mobile = ?)"
				,new PreparedStatementSetter(){
					@Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setString(1, nickname);
						ps.setLong(2, Long.valueOf(mobileNumber.toString()));
					}
				});		
	}

	@Override
	public void savePasswordDigest(final MobileNumber mobileNumber, final String passwordDigest) {
		mysqlDataSource.update("update duoshouji.user set password = ? where mobile = ?)"
				,new PreparedStatementSetter(){
					@Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setString(1, passwordDigest);
						ps.setLong(2, Long.valueOf(mobileNumber.toString()));
					}
				});	
	}

	@Override
	public long createNote(MobileNumber mobileNumber,
			final NotePublishAttributes noteAttributes) {
		final long userId = getUserId(mobileNumber);
		mysqlDataSource.update("insert into duoshouji.note (title, content, create_time) values(?,?,?))"
				,new PreparedStatementSetter(){
					@Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setString(1, noteAttributes.getTitle());
						ps.setString(2, noteAttributes.getContent());
						ps.setLong(3, System.currentTimeMillis());
					}
				});
		
		return mysqlDataSource.query("select max(id) note_id from duoshouji.note where user_id = " + userId
				, new ResultSetExtractor<Long>(){
					@Override
					public Long extractData(ResultSet rs) throws SQLException,
							DataAccessException {
						rs.next();
						return rs.getLong("note_id");
					}
				});
	}
	
	private long getUserId(MobileNumber mobileNumber) {
		return mysqlDataSource.query("select id from duoshouji.user where mobile = " + mobileNumber
				, new ResultSetExtractor<Long>(){
					@Override
					public Long extractData(ResultSet rs) throws SQLException,
							DataAccessException {
						rs.next();
						return rs.getLong("id");
					}
					
				}).longValue();
	}
}