function normalize(filein,fileout)
	p = csvread(filein);
	a = min(p(:));
	b = max(p(:));
	ra = 1;
	rb = -1;
	pa = (((ra-rb) * (p - a)) / (b - a)) + rb;
	csvwrite(fileout,pa);
end
