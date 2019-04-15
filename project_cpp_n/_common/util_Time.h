#pragma once
#ifndef __UTIL_TIME_H__
#define __UTIL_TIME_H__

//
#include <windows.h>
#include <time.h>
#include <sys/timeb.h>
#include <sqltypes.h>

#include <string>

//
const int SEC_A_SEC = 1;
const int SEC_A_MIN = SEC_A_SEC * 60;
const int SEC_A_HOUR = SEC_A_MIN * 60;
const int SEC_A_DAY = SEC_A_HOUR * 24;
const int SEC_A_WEEK = SEC_A_DAY * 7;
const int SEC_A_YEAR = SEC_A_DAY * 365;

const INT64 MILLISEC_A_SEC = 1000;
const INT64 MILLISEC_A_MIN = MILLISEC_A_SEC * 60;
const INT64 MILLISEC_A_HOUR = MILLISEC_A_MIN * 60;
const INT64 MILLISEC_A_DAY = MILLISEC_A_HOUR * 24;
const INT64 MILLISEC_A_WEEK = MILLISEC_A_DAY * 7;
const INT64 MILLISEC_A_YEAR = MILLISEC_A_DAY * 365;

//
int GetTimeSec();
INT64 GetTimeMilliSec();

class CPerformanceCheck
{
private:
	std::wstring m_wstrComment = {};

	bool m_bUsePerformanceCount = true;
	INT64 m_biPerformanceCount[2] = {0,};
	INT64 m_biPerformance = 0;

public:
	CPerformanceCheck(std::wstring wstrComment);
	~CPerformanceCheck();
};

//
class CTimeSet
{
private:
	__time64_t m_tGMT;
	__time64_t m_tLocalT;

	struct tm m_tmGMT;
	struct tm m_tmLocalT;

	//
public:
	CTimeSet();
	CTimeSet(__time64_t tTime, bool bGMT);
	CTimeSet(int nYear, int nMonth, int nDay, int nHour, int nMin, int nSec, bool bGMT);
	CTimeSet(TIMESTAMP_STRUCT dbTime);

	void SetTime();
	void SetTime(__time64_t tTime, bool bGMT);
	void SetTime(int nYear, int nMonth, int nDay, int nHour, int nMin, int nSec, bool bGMT);
	void SetTime(TIMESTAMP_STRUCT dbTime);

	__time64_t ConvertLC2GM(__time64_t tLocal);
	__time64_t ConvertLC2GM(struct tm tmLocal);

	__time64_t GetTime_GM() { return m_tGMT; }
	__time64_t GetTime_LC() { return m_tLocalT; }
	struct tm GetTimeST_GM() { return m_tmGMT; }
	struct tm GetTimeST_LC() { return m_tmLocalT; }

	//
	int GetYear(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_year : m_tmGMT.tm_year ) + 1900; }
	int GetMonth(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_mon + 1 : m_tmGMT.tm_mon + 1 ); }
	int GetDay(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_mday : m_tmGMT.tm_mday ); }
	int GetHour(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_hour : m_tmGMT.tm_hour ); }
	int GetMin(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_min : m_tmGMT.tm_min ); }
	int GetSec(bool bLocal = true) { return ( bLocal ? m_tmLocalT.tm_sec : m_tmGMT.tm_sec ); }

	//
	void _test();
};

//
#endif //__UTIL_TIME_H__