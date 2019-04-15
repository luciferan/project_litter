class uniqueIndexMaker
{
	long biIndex = 0;
	
	synchronized public long getUniqueIndex()
	{
		return (biIndex += 1);
	}
}