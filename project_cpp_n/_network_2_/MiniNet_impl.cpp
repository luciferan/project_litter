#include "../_network_2_/MiniNet.h"
#include "../_network_2_/Connector.h"
#include "../_common/Log.h"
#include "../_common/util.h"
#include <string>

//
DWORD CMiniNet::DoUpdate(INT64 biCurrTime)
{
	if( m_biUpdateTime < biCurrTime )
	{
		m_biUpdateTime = GetTimeMilliSec() + (MILLISEC_A_MIN);
		
		//
		wstring wstrReport = {};
		wstrReport.append(L"ConnectorState: ");
		wstrReport.append(CConnectorMgr::GetInstance().GetStateReport());

		g_PerformanceLog.Write(wstrReport.c_str());
	}

	return RESULT_SUCC;
}