object :- N : scene:<e,t>
thing :- N : scene:<e,t>
things :- N : scene:<e,t>
object :- N/N : (lambda $0:<e,t> $0)
object that is :- N/N : (lambda $0:<e,t> $0)

other :- N/N : (lambda $0:<e,t> $0)

what :- (S\N)/N : (lambda $0:a (lambda $1:e (query:<a,<e,p>> $0 $1)))
color :- N : color:a
what :- S/N : (lambda $0:p $0)

cylinders :- N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> shape:a $0 cylinder:p))
cylinders :- N : (filter:<a,<<e,t>,<p,<e,t>>>> shape:a scene:<e,t> cylinder:p)
sphere :- N : (filter:<a,<<e,t>,<p,<e,t>>>> shape:a scene:<e,t> sphere:p)
cubes :- N : (filter:<a,<<e,t>,<p,<e,t>>>> shape:a scene:<e,t> cube:p)

// "are there other objects that X"
// vs. "are there cylinders that Y"
// -- this allows us to capture them with a single pattern
other objects :- N : (lambda $0:<e,t> $0)

metallic :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> material:a $0 metal:p))
matte :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> material:a $0 rubber:p))
shiny :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> material:a $0 metal:p))
big :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> size:a $0 large:p))
green :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 green:p))
brown :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 brown:p))
purple :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 purple:p))
cyan :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 cyan:p))
yellow :- N/N : (lambda $0:<e,t> (filter:<a,<<e,t>,<p,<e,t>>>> color:a $0 yellow:p))

same :- N/N/N : (lambda $0:e (lambda $1:a (same:<a,<e,<e,t>>> $0 $1)))
shape :- N : shape:a
size :- N : size:a
material :- N : material:a
as :- N/N : (lambda $0:e $0)

// "the X of Y"
of :- N\N/N : (lambda $0:e (lambda $1:a (query:<a,<e,p>> $1 $0)))

the :- S/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))
the :- N/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))
// "[of] the same shape"
the :- N/N : (lambda $0:<e,t> $0)
of the :- N/N : (lambda $0:<e,t> $0)

either :- N/N : (lambda $0:<e,t> $0)
either :- S/N : (lambda $0:<e,t> $0)
or :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (union:<<e,t>,<<e,t>,<e,t>>> $0 $1)))
and :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (intersect:<<e,t>,<<e,t>,<e,t>>> $0 $1)))

what is :- S/N : (lambda $0:e $0)
are there any other things :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
that are :- S\(S/N)/N : (lambda $0:<e,t> (lambda $1:<<e,t>,t> ($1 $0)))
is there a :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
what number of :- S/N/S : (lambda $0:<<e,t>,<e,t>> (lambda $1:<e,t> (count:<<e,t>,i> ($0 $1))))
are :- S/N : (lambda $0:<e,t> $0)
right of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 right:s))
left of :- N/N : (lambda $0:e (relate:<e,<s,<e,t>>> $0 left:s))
// are there more .. than ..
than :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (greater_than:<i,<i,t>> (count:<<e,t>,i> $1) (count:<<e,t>,i> $0)))
are there more :- S/N : (lambda $0:t $0)
