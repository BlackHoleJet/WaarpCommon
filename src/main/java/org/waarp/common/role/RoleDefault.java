/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.role;

/**
 * Role to be used in Waarp projects
 * 
 * FIXME not functional for the moment: early stage development
 * 
 * @author Frederic Bregier
 * 
 */
public class RoleDefault {
	public static enum ROLE {
		NOACCESS(0), 
		READONLY(1), TRANSFER(2), 
		RULE(4), HOST(8), 
		LIMIT(16), SYSTEM(32), LOGCONTROL(64), 
		UNUSED(-128),
		PARTNER(READONLY, TRANSFER),
		CONFIGADMIN(PARTNER, RULE, HOST),
		FULLADMIN(CONFIGADMIN, LIMIT, SYSTEM, LOGCONTROL);
		
		private byte role;
		private ROLE(int val) {
			this.role = (byte) val;
		}
		private ROLE(ROLE...name) {
			for (ROLE role : name) {
				this.role |= role.role;
			}
		}
		private boolean isContained(byte value) {
			return (value & role) != 0;
		}
		public static String toString(byte fromRole) {
			StringBuilder result = new StringBuilder("[ ");
			ROLE [] values = ROLE.values();
			for (ROLE role : values) {
				if (role.isContained(fromRole)) {
					result.append(role.name());
					result.append(' ');
				}
			}
			result.append(']');
			return result.toString();
		}
	};

	private byte role;

	public RoleDefault() {
		this.role = ROLE.NOACCESS.role;
	}

	public RoleDefault(ROLE role) {
		this.role = role.role;
	}

	@Override
	public String toString() {
		return ROLE.toString(role);
	}
	
	public void addRole(ROLE newrole) {
		this.role |= newrole.role;
	}

	public void setRole(ROLE newrole) {
		this.role = newrole.role;
	}

	public void setRole(RoleDefault newrole) {
		this.role = newrole.role;
	}

	public void clear() {
		this.role = ROLE.NOACCESS.role;
	}
	
	public boolean isContaining(ROLE otherrole) {
		return otherrole.isContained(role);
	}

	public boolean hasReadOnly() {
		return ROLE.READONLY.isContained(role);
	}

	public boolean hasTransfer() {
		return ROLE.TRANSFER.isContained(role);
	}

	public boolean hasRule() {
		return ROLE.RULE.isContained(role);
	}

	public boolean hasHost() {
		return ROLE.HOST.isContained(role);
	}

	public boolean hasLimit() {
		return ROLE.LIMIT.isContained(role);
	}

	public boolean hasSystem() {
		return ROLE.SYSTEM.isContained(role);
	}

	public boolean hasUnused() {
		return ROLE.UNUSED.isContained(role);
	}

	public boolean hasLogControl() {
		return ROLE.LOGCONTROL.isContained(role);
	}

	public static boolean HasReadOnly(byte role) {
		return ROLE.READONLY.isContained(role);
	}

	public static boolean HasTransfer(byte role) {
		return ROLE.TRANSFER.isContained(role);
	}

	public static boolean HasRule(byte role) {
		return ROLE.RULE.isContained(role);
	}

	public static boolean HasHost(byte role) {
		return ROLE.HOST.isContained(role);
	}

	public static boolean HasLimit(byte role) {
		return ROLE.LIMIT.isContained(role);
	}

	public static boolean HasSystem(byte role) {
		return ROLE.SYSTEM.isContained(role);
	}

	public static boolean HasUnused(byte role) {
		return ROLE.UNUSED.isContained(role);
	}

	public static boolean HasLogControl(byte role) {
		return ROLE.LOGCONTROL.isContained(role);
	}

}
