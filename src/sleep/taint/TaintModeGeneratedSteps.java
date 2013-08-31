/* 
 * Copyright (C) 2002-2012 Raphael Mudge (rsmudge@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package sleep.taint;

import java.util.List;

import sleep.engine.GeneratedSteps;
import sleep.engine.Step;

/**
 * A replacement factory that generates Sleep interpreter instructions that
 * honor and spread the taint mode.
 */
public class TaintModeGeneratedSteps extends GeneratedSteps {
	
	@Override
	public Step Call(final String function) {
	
		return new TaintCall(function, super.Call(function));
	}
	
	@Override
	public Step PLiteral(final List doit) {
	
		return new PermeableStep(super.PLiteral(doit));
	}
	
	@Override
	public Step Operate(final String oper) {
	
		return new TaintOperate(oper, super.Operate(oper));
	}
	
	@Override
	public Step ObjectNew(final Class name) {
	
		return new PermeableStep(super.ObjectNew(name));
	}
	
	@Override
	public Step ObjectAccess(final String name) {
	
		return new TaintObjectAccess(super.ObjectAccess(name), name, null);
	}
	
	@Override
	public Step ObjectAccessStatic(final Class aClass, final String name) {
	
		return new TaintObjectAccess(super.ObjectAccessStatic(aClass, name), name, aClass);
	}
}
