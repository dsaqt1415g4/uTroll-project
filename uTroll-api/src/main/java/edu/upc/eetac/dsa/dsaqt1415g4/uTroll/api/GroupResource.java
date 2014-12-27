package edu.upc.eetac.dsa.dsaqt1415g4.uTroll.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.digest.DigestUtils;

import com.mysql.jdbc.Statement;

import edu.upc.eetac.dsa.dsaqt1415g4.uTroll.api.model.Comment;
import edu.upc.eetac.dsa.dsaqt1415g4.uTroll.api.model.Group;

@Path("/groups")
public class GroupResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();

	private final static String GET_GROUP_BY_GROUPID_QUERY = "select * from groups where groupid=?";
	private final static String CREATE_GROUP_QUERY = "insert into groups (groupname, price, ending_timestamp, creator, state) values(?, ?, ?, ?, ?)";
	private final static String UPDATE_GROUP_QUERY = "update groups set state = ? where groupid = ?";
	private final static String VALIDATE_CREATOR = "select groupid from users where username = ?";
	private final static String UPDATE_USER_GROUP_QUERY = "update users set groupid = ? where username = ?";
	private final static String VALIDATE_USER = "select groupname from groups where groupid = ? and creator = ?";

	// Método obtención grupo por id
	@GET
	@Path("/{groupid}")
	@Produces(MediaType.UTROLL_API_GROUP)
	public Group getUser(@PathParam("groupid") int groupid) {
		Group group = new Group();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_GROUP_BY_GROUPID_QUERY);
			stmt.setInt(1, groupid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				group.setCreationTimestamp(rs.getLong("creation_timestamp"));
				group.setEndingTimestamp(rs.getLong("ending_timestamp"));
				group.setGroupid(rs.getInt("groupid"));
				group.setGroupname(rs.getString("groupname"));
				group.setPrice(rs.getInt("price"));
				group.setState(rs.getString("state"));
				group.setCreator(rs.getString("creator"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		return group;
	}

	// Crear un nuevo grupo
	@POST
	@Consumes(MediaType.UTROLL_API_GROUP)
	@Produces(MediaType.UTROLL_API_GROUP)
	public Group createGroup(Group group) {
		validateCreator(); // Comprobar que no pertenezco ya a un grupo

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(CREATE_GROUP_QUERY,
					Statement.RETURN_GENERATED_KEYS);

			stmt.setString(1, group.getGroupname());
			stmt.setInt(2, group.getPrice());
			// stmt.setLong(3, group.getCreationTimestamp());
			stmt.setLong(3, group.getEndingTimestamp());
			stmt.setString(4, security.getUserPrincipal().getName());
			stmt.setString(5, "open");

			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				int groupid = rs.getInt(1);

				group = getGroupFromDatabase(groupid);
			} else {
				// Something has failed...
			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		updateUserGroup(group.getGroupid());

		return group;
	}

	// Modificar el estado de un grupo
	@PUT
	@Path("/{groupid}")
	@Consumes(MediaType.UTROLL_API_GROUP)
	@Produces(MediaType.UTROLL_API_GROUP)
	public Group updateGroup(@PathParam("groupid") int groupid, Group group) {
		validateUser(groupid);

		validateUpdateGroup(group);

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(UPDATE_GROUP_QUERY);
			stmt.setString(1, group.getState());
			stmt.setInt(2, groupid);

			int rows = stmt.executeUpdate();
			if (rows == 1)
				group = getGroupFromDatabase(groupid);
			else {
				throw new NotFoundException("Group not found");
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return group;
	}

	// Recuperar grupo de la base de datos por su ID
	private Group getGroupFromDatabase(int groupid) {
		Group group = new Group();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_GROUP_BY_GROUPID_QUERY);
			stmt.setInt(1, groupid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				group.setCreationTimestamp(rs.getLong("creation_timestamp"));
				group.setEndingTimestamp(rs.getLong("ending_timestamp"));
				group.setGroupid(rs.getInt("groupid"));
				group.setGroupname(rs.getString("groupname"));
				group.setPrice(rs.getInt("price"));
				group.setState(rs.getString("state"));
				group.setCreator(rs.getString("creator"));
			} else
				throw new NotFoundException("Group not found.");
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return group;
	}

	// Método para asignarle a un usuario el grupo que ha creado
	private void updateUserGroup(int groupid) {
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(UPDATE_USER_GROUP_QUERY);
			stmt.setInt(1, groupid);
			stmt.setString(2, security.getUserPrincipal().getName());

			int rows = stmt.executeUpdate();
			if (rows != 1)
				throw new NotFoundException("user not found");
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	// Método para validar que no pertenezco ya a un grupo
	private void validateCreator() {
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(VALIDATE_CREATOR);
			stmt.setString(1, security.getUserPrincipal().getName());
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int groupid = rs.getInt("groupid");
				if (groupid != 0)
					throw new BadRequestException(
							"You already belong to a group");
			} else {

			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	// Método para validar que soy el creador del grupo que quiero modificar
	private void validateUser(int groupid) {
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(VALIDATE_USER);
			stmt.setInt(1, groupid);
			stmt.setString(2, security.getUserPrincipal().getName());
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {

			} else {
				throw new BadRequestException(
						"You are not the creator of this group");
			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	// Método para validar el uso de uno de los estados por defecto
	private void validateUpdateGroup(Group group) {
		if (group.getState().equals("open")) {

		} else if (group.getState().equals("closed")) {

		} else if (group.getState().equals("active")) {

		} else {
			throw new BadRequestException("The state is not valid");
		}
	}

	// Para trabajar con parámetros de seguridad
	@Context
	private SecurityContext security;
}
