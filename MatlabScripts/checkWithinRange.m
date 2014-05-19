function[b] = checkWithinRange(a,p,t)
    [m,n]=size(p);
    
    for i=1:m
        j=p([i],[1,2,3]);
        x=double(a);
        y=double(j([1],[1]));
        z=double(j([1],[2]));
        if x > y && x <= z
            b=strcat('d',num2str(double(j([1],[3]))));
            return;
        end
    end
end