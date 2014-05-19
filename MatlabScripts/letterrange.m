function letterrange(csvfile,csvfileout,rangefile)

	t=csvread(csvfile);

	p=csvread (rangefile);

	[m,n] = size(t);
	A=repmat({''},m,n);
	for i=1:m
		for j=1:n
			k=t([i],[j]);
			b=checkWithinRange(k,p,t);
			A{i,j}=(b);
		end
	end
	cell2csv(csvfileout,A,',','2000','.');
end

