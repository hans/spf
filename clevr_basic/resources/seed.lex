object :- N : scene:<e,t>
thing :- N : scene:<e,t>
object :- N/N : (lambda $0:<e,t> $0)
object that is :- N/N : (lambda $0:<e,t> $0)

other :- N/N : (lambda $0:<e,t> $0)

what :- (S\N)/N : (lambda $0:a (lambda $1:e (query:<a,<e,p>> $0 $1)))
color :- N : color:a
what :- S/N : (lambda $0:p $0)

cylinders :- N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> shape:a $0 cylinder:p))
sphere :- N : (filter:<a,<<e,t>,<p,<e,t>>>> shape:a scene:<e,t> sphere:p)
other objects :- N : (lambda $0:<e,t> $0)

metallic :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> material:a $0 metal:p))
shiny :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> material:a $0 metal:p))
big :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> size:a $0 large:p))
green :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 green:p))
brown :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 brown:p))
purple :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 purple:p))

same :- N/N/N : (lambda $0:a (lambda $1:e (lambda $2:e (same:<a,<e,<e,t>>> $0 $1))))
shape :- N : shape:a
as :- N/N : (lambda $0:e $0)

the :- S/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))
the :- N/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))

either :- S/N : (lambda $0:<e,t> $0)
or :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (union:<<e,t>,<<e,t>,<e,t>>> $0 $1)))
and :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (intersect:<<e,t>,<<e,t>,<e,t>>> $0 $1)))

// other things that are the same shape as :- N/N : (lambda $0:e (same:<a,<e,<e,t>>> shape:a $0))
the same size as :- N/N : (lambda $0:e (same:<a,<e,<e,t>>> size:a $0))

are there any :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
is there a :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
what number of :- S/N/S : (lambda $0:<<e,t>,<e,t>> (lambda $1:<e,t> (count:<<e,t>,i> ($0 $1))))
are :- S/S : (lambda $0:<e,t> $0)
right of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 right:s))
left of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 left:s))
// are there more .. than ..
than :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (greater_than:<i,<i,t>> (count:<<e,t>,i> $1) (count:<<e,t>,i> $0)))
are there more :- S/N : (lambda $0:t $0)
