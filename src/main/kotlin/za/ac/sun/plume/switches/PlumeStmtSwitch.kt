package za.ac.sun.plume.switches

import soot.jimple.*

class PlumeStmtSwitch : AbstractStmtSwitch() {
    override fun caseAssignStmt(stmt: AssignStmt?) {
        super.caseAssignStmt(stmt)
    }

    override fun caseBreakpointStmt(stmt: BreakpointStmt) {

    }

    override fun caseEnterMonitorStmt(stmt: EnterMonitorStmt?) {
        super.caseEnterMonitorStmt(stmt)
    }

    override fun caseExitMonitorStmt(stmt: ExitMonitorStmt?) {
        super.caseExitMonitorStmt(stmt)
    }

    override fun caseGotoStmt(stmt: GotoStmt?) {
        super.caseGotoStmt(stmt)
    }

    override fun caseIdentityStmt(stmt: IdentityStmt?) {
        super.caseIdentityStmt(stmt)
    }

    override fun caseIfStmt(stmt: IfStmt?) {
        super.caseIfStmt(stmt)
    }

    override fun caseInvokeStmt(stmt: InvokeStmt?) {
        super.caseInvokeStmt(stmt)
    }

    override fun caseLookupSwitchStmt(stmt: LookupSwitchStmt?) {
        super.caseLookupSwitchStmt(stmt)
    }

    override fun caseNopStmt(stmt: NopStmt?) {
        super.caseNopStmt(stmt)
    }

    override fun caseRetStmt(stmt: RetStmt?) {
        super.caseRetStmt(stmt)
    }

    override fun caseReturnStmt(stmt: ReturnStmt?) {
        super.caseReturnStmt(stmt)
    }

    override fun caseReturnVoidStmt(stmt: ReturnVoidStmt?) {
        super.caseReturnVoidStmt(stmt)
    }

    override fun caseTableSwitchStmt(stmt: TableSwitchStmt?) {
        super.caseTableSwitchStmt(stmt)
    }

    override fun caseThrowStmt(stmt: ThrowStmt?) {
        super.caseThrowStmt(stmt)
    }
}