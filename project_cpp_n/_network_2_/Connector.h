#pragma once
#ifndef __CONNECTOR_H__
#define __CONNECTOR_H__

//
#include <winsock2.h>
#include <WS2tcpip.h>
#include <Windows.h>

#include <unordered_map>
#include <queue>
#include <list>

#include "MiniNet_Header.h"
#include "Buffer.h"

#include "../_common/SafeLock.h"
#include "../_common/util_String.h"

//
using namespace std;
class CConnector;

//
struct OVERLAPPED_EX
{
	OVERLAPPED overlapped = {0,};

	CConnector *pSession = nullptr;
	CNetworkBuffer *pBuffer = nullptr;

	//
	void ResetOverlapped() { memset(&overlapped, 0, sizeof(OVERLAPPED)); }
};

//
class CConnector 
{
protected:
	DWORD m_dwUniqueIndex = 0;
	//DWORD m_dwUsed = 0;
	//DWORD m_dwActive = 0;

	void *m_pParam = nullptr;

	SOCKET m_Socket = INVALID_SOCKET;

public:
	SOCKADDR_IN m_SockAddr = {0,};

	char m_szDomain[eNetwork::MAX_LEN_DOMAIN_STRING + 1] = {0,};
	WCHAR m_wcsDomain[eNetwork::MAX_LEN_DOMAIN_STRING + 1] = {0,};
	WORD m_wPort = 0;

	// send
	OVERLAPPED_EX m_SendRequest; 
	CLock m_SendQueueLock;
	queue<CNetworkBuffer*> m_SendQueue = {}; // �����ִ°� send �մϴ�. �ִ� ������ �Ѿ�� ���۷��� ���� �����ߵǴ� ��Ŷ�� ������
	DWORD m_dwSendRef = 0;

	// recv
	OVERLAPPED_EX m_RecvRequest;
	CCircleBuffer m_RecvDataBuffer; // recv ������ ���⿡ �׽��ϴ�. �����Ǹ� ������ ��Ʈ���ϴ�.
	DWORD m_dwRecvRef = 0;

	// inner
	OVERLAPPED_EX m_InnerRequest;
	CLock m_InnerQueueLock;
	queue<CNetworkBuffer*> m_InnerQueue = {}; //
	DWORD m_dwInnerRef = 0;

	//
	INT64 m_biUpdateTimer = 0;
	INT64 m_biHeartbeatTimer = 0;

	//
public:
	CConnector(DWORD dwUniqueIndex = 0);
	virtual ~CConnector();

	bool Initialize();
	bool Finalize();

	DWORD GetUniqueIndex() { return m_dwUniqueIndex; }
	DWORD GetIndex() { return m_dwUniqueIndex; }
	
	//void SetActive() { InterlockedExchange(&m_dwActive, 1); }
	//void SetDeactive() { InterlockedExchange(&m_dwActive, 0); }
	//DWORD GetActive() { return InterlockedExchange(&m_dwActive, m_dwActive); }

	void* SetParam(void *pParam) { return m_pParam = pParam; }
	void* GetParam() { return m_pParam; }
	bool GetUsed() { return (nullptr != m_pParam); }

	SOCKET SetSocket(SOCKET socket) { return m_Socket = socket; }
	SOCKET GetSocket() { return m_Socket; }
	bool GetActive() { return (INVALID_SOCKET != m_Socket); }

	void SetDomain(WCHAR *pwcsDomain, WORD wPort);
	void GetSocket2IP(char *pszIP);
	void ConvertSocket2IP();

	char* GetDomainA() { return m_szDomain; }
	WCHAR* GetDomain() { return m_wcsDomain; }
	WORD GetPort() { return m_wPort; }

	// send
	eResultCode AddSendData(char *pSendData, DWORD dwSendDataSize);
	int AddSendQueue(char *pSendData, DWORD dwSendDataSize); // ret: sendqueue.size
	int SendPrepare(); //ret: send data size
	int SendComplete(DWORD dwSendSize); // ret: remain send data size

	WSABUF* GetSendWSABuffer();
	OVERLAPPED* GetSendOverlapped() { return (OVERLAPPED*)&m_SendRequest; }
	DWORD IncSendRef() { InterlockedIncrement(&m_dwSendRef); return m_dwSendRef; }
	DWORD DecSendRef() { InterlockedDecrement(&m_dwSendRef); return m_dwSendRef; }
	DWORD GetSendRef() { return m_dwSendRef; }

	// recv
	int RecvPrepare(); //ret: recv buffer size
	int RecvComplete(DWORD dwRecvSize); // ret: recvdatabuffer.getdatasize
	virtual int DataParsing();

	WSABUF* GetRecvWSABuffer();
	OVERLAPPED* GetRecvOverlapped() { return (OVERLAPPED*)&m_RecvRequest; }
	DWORD IncRecvRef() { InterlockedIncrement(&m_dwRecvRef); return m_dwRecvRef; }
	DWORD DecRecvRef() { InterlockedDecrement(&m_dwRecvRef); return m_dwRecvRef; }
	DWORD GetRecvRef() { return m_dwRecvRef; }

	// inner
	int AddInnerQueue(char *pSendData, DWORD dwSendDataSize);
	int InnerPrepare();
	int InnerComplete(DWORD dwInnerSize);

	WSABUF* GetInnerWSABuffer();
	OVERLAPPED* GetInnerOverlapped() { return (OVERLAPPED*)&m_InnerRequest; }
	DWORD IncInnerRef() { InterlockedIncrement(&m_dwInnerRef); return m_dwInnerRef; }
	DWORD DecInnerRef() { InterlockedDecrement(&m_dwInnerRef); return m_dwInnerRef; }
	DWORD GetInnerRef() { return m_dwInnerRef; }

	//
	bool CheckUpdateTimer(INT64 biCurrTime) { return (m_biUpdateTimer < biCurrTime); }
	bool DoUpdate(INT64 biCurrTime);
	bool CheckHeartbeat(INT64 biCurrTime);
	wstring GetStateReport();
};

//
class CConnectorMgr 
{
private:
	DWORD m_dwConnectorIndex = 0;
	DWORD m_dwMaxConnectorCount = 0;

	list<CConnector> m_ConnectorList = {}; // ������ ��ü ����Ʈ
	list<CConnector*> m_FreeConnectorList = {}; // ����Ҽ��ִ�

public:
	list<CConnector*> m_UsedConnectorList = {}; // �������
	CLock m_Lock;

	//
private:
	CConnectorMgr::CConnectorMgr(int nConnectorMax = 2000)
	{
		for( int cnt = 0; cnt < nConnectorMax; ++cnt )
		{
			CConnector *pData = new CConnector(GetUniqueIndex());
			
			m_ConnectorList.push_back(*pData);
			m_FreeConnectorList.push_back(pData);
		}

		m_dwMaxConnectorCount = nConnectorMax;
	}

	CConnectorMgr::~CConnectorMgr(void)
	{
		m_UsedConnectorList.clear();
		m_FreeConnectorList.clear();
		m_ConnectorList.clear();
	}

public:
	static CConnectorMgr& GetInstance()
	{
		static CConnectorMgr *pInstance = new CConnectorMgr();
		return *pInstance;
	}
	
	DWORD GetUniqueIndex() 
	{
		InterlockedIncrement((DWORD*)&m_dwConnectorIndex); 
		return m_dwConnectorIndex; 
	}

	CConnector* GetFreeConnector()
	{
		CConnector *pConnector = nullptr;

		{
			CScopeLock lock(m_Lock);

			if( m_FreeConnectorList.size() )
			{
				pConnector = m_FreeConnectorList.front();
				m_FreeConnectorList.pop_front();

				m_UsedConnectorList.push_back(pConnector);
			}
		}

		return pConnector;
	}

	void ReleaseConnector(CConnector *pConnector)
	{
		{
			CScopeLock lock(m_Lock);

			//pConnector->SetDeactive();

			if( std::find(m_UsedConnectorList.begin(), m_UsedConnectorList.end(), pConnector) != m_UsedConnectorList.end() )
			{
				m_UsedConnectorList.remove(pConnector);
				m_FreeConnectorList.push_back(pConnector);
			}
		}
	}

	wstring GetStateReport()
	{
		wstring wstrState = {};

		{
			CScopeLock lock(m_Lock);
			wstrState.append(FormatW(L"pool:%d, used:%d, free:%d", m_ConnectorList.size(), m_UsedConnectorList.size(), m_FreeConnectorList.size()));
		}

		return wstrState;
	}
};

//
#endif //__CONNECTOR_H__