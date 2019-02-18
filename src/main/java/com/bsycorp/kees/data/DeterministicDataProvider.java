package com.bsycorp.kees.data;

import com.bsycorp.kees.gpg.GPGKeyGenerator;
import com.bsycorp.kees.models.Parameter;
import com.bsycorp.kees.models.SecretTypeEnum;
import java.io.IOException;
import java.security.NoSuchProviderException;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;

public class DeterministicDataProvider extends AbstractDataProvider {

    private static Logger LOG = LoggerFactory.getLogger(DeterministicDataProvider.class);

    private static final ArrayList<String[]> ENCODED_PRECALCULATED_VALUES = new ArrayList<>();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    static {
        //use pregenerated keys for everything, still have different keys so we still test key assignment code paths
        //when they were being deterministically generated init containers were taking along time to start because of all the RSA algo compute
        //this class is only used when local-mode=true
        ENCODED_PRECALCULATED_VALUES.add(new String[]{
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhVSvhBQWn5HjGDJDeYUcNs9/c6kai0HXMdPzlVvFEUJqn+1dOEgQOwI7nC+QeH+2nb25cv1nEMP7igYKGX8KoeXzo08lItXOVU3xH2srF905ZNgWM1Ps9pnq3ggkWLvJ++8MGeEzY48A2NfQlvRtlv4oS5bfIIRZQac2rKanJRs4XxVk3mFIrBazCXdz5nGY337C0Hgwk99En7rXkb4zD7Yy7uIwZSmL5dsIPTlpIb/8HkOr3yODSqy9ORuQKHhgOg2tjJYR8y+mIfi+UZGfAGyF0aeS/8y9CrkoGGCODdBE+tR6kUwQhiER2ogxk16KhW2+N74asDTz4+0lMzGGXQIDAQAB",
                "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCFVK+EFBafkeMYMkN5hRw2z39zqRqLQdcx0/OVW8URQmqf7V04SBA7AjucL5B4f7advbly/WcQw/uKBgoZfwqh5fOjTyUi1c5VTfEfaysX3Tlk2BYzU+z2mereCCRYu8n77wwZ4TNjjwDY19CW9G2W/ihLlt8ghFlBpzaspqclGzhfFWTeYUisFrMJd3PmcZjffsLQeDCT30SfuteRvjMPtjLu4jBlKYvl2wg9OWkhv/weQ6vfI4NKrL05G5AoeGA6Da2MlhHzL6Yh+L5RkZ8AbIXRp5L/zL0KuSgYYI4N0ET61HqRTBCGIRHaiDGTXoqFbb43vhqwNPPj7SUzMYZdAgMBAAECggEAPLKH0J4AipY0hxBizVB+vqkJwZKrRdgsaj+uNUF43lmZBsNdW8DaurJShtDUeTbS4M69PQ8FO4ms2nHTaTX/liKEHsVqB4jEDOuVqJL2JEAO38zLloRPbRg25utX+JlM44c4wBNYfKteXFkrWX1e4o1DtN2zKC94Hr5L0p9vmiE4zst6TmNP07EJdIpKgjcEEPxOVS9fGsl/VxwEhutHYb5JGFqypSIP93GmYr+Z4NlwZ69VTqyMKM0vdShKpv5Q87vEahoUEv6uP+Bzf+zms+R1tmN+Zhy1q+Eq2gyBIOgv8+SFHHJyYW5SAmAiT5Jvyk9YW3hro7ox0025syXkgQKBgQDBSkSYPWQb+hQh9qmxMDnFefVEYRPGjaJYspOZEa4UkUACRaF0Kd2Aq79T8pH1JuYQWOi9vqW6f29UeAo232X0eJfMVg+FWMPYL3OGaU2zhjiihKLVoso8LgfqINBTOUQtoJj4YpWuMlIUfbqOX2xpjHuj286EousxtG6JYACqoQKBgQCwln2cOi4tFwwBBvb9VpM0ubwOYusvnYiBoCg76NMDvV5NpAMO92120otoMjMhS0hKRGaVJZSyHwRl4HLraMtf59OPhwp/we2OnkeDOVJ1XA7SyxCOkYnwtEVYIJERtXdCR7hfT74gDWzQ7TQm2Q3xmf4RK8RHfvStiVhZHMoePQKBgQCuhIPSgPt2VyD+WVt1Y/mFV0wf5RaZ2x+Nvg6N0ey/HTYR1xSjcXlQx8ED5qvqEKkAcYYSa6Pu80htl8SBLss7I/bKw5lX6IFLG+zOmx1QzgsskV0ETusR0u3Kcr4lpjrsh2gWO8yxbzW00qAyE+4qjnDDzZ51GzQHuMVku41egQKBgQCOwNnClfYGVX9KxUqd+oL+OLgiGS1vTLaxs7tS0yDjAQ9fTJt8WeGQERzJmutimu1RvIIBSGQwqopvoUbFr+3ZFhwdB1ohdbCJfsLo/kn+vfDtz2MPHfCZOeJlH86mtkA2wKv3wQs99hxluZxTW1y+V3LghrUh5yv9re5R+8gZMQKBgByCHU4TTdMIub92xm2YJB3pq6EtwoJRD96sgGUZm+ntjXLjuuA7gj5D1hAVW5ZY8samSdKgn+9OsmOoGrdT8bH6SWgOzYOIo8po6tyPTrsYnoWBptqA/Lle2iEyQLwyFUVU4tr93FYzEqKfC+g6OvVx/F15aVLkyT7TmR+BuPWQ"
        });
        ENCODED_PRECALCULATED_VALUES.add(new String[]{
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmnbS+d+8b1LpqwC+dztxsNAoIUt+iMQ6NnkSs8h7HvosDzWm72QjaqGy16Ixt2rHU0G3Xv2ur4z3BLaG7zE5k0liBaW2dP3zdZQfnGlf6yTmSTPjr7R1lo1el8fzOw67NuVBUtCe/gXnpYhYX8v5McCvGSpca1AXfB/rMaIZk2mH7Jk8g2QhnESTerCNkeFGUFHmErQ2EaGyP1X4pVmlJTdk+dD6hEF1WyvwQBsvT+eW5a45eRblpTGx0wTeIt2Ht4cGsrkhmKmVPiUJxEOf3VBK0MtkVgF8ekODjmQQ2+lC9vTi+c8CIJSKQhHPcn9TMsTucisg/vqcMMyzybsUGwIDAQAB",
                "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCadtL537xvUumrAL53O3Gw0CghS36IxDo2eRKzyHse+iwPNabvZCNqobLXojG3asdTQbde/a6vjPcEtobvMTmTSWIFpbZ0/fN1lB+caV/rJOZJM+OvtHWWjV6Xx/M7Drs25UFS0J7+BeeliFhfy/kxwK8ZKlxrUBd8H+sxohmTaYfsmTyDZCGcRJN6sI2R4UZQUeYStDYRobI/VfilWaUlN2T50PqEQXVbK/BAGy9P55blrjl5FuWlMbHTBN4i3Ye3hwayuSGYqZU+JQnEQ5/dUErQy2RWAXx6Q4OOZBDb6UL29OL5zwIglIpCEc9yf1MyxO5yKyD++pwwzLPJuxQbAgMBAAECggEBAIyWzlxEOfyRWMEeQj2/yzEFpSD418if4eQmutEMDpQZW/TT/ocxe9LYLjF6HQo2lAnBbKd2+oIqKcMOZy5afW8DgcZmF/XQFsHT+hmhB3687SV/pyMLe2N2dLxtb4M3W0sRyy0XWT8YHMlbmiRHQR75o+Wh2tCJ63Y8jxNz2ReDqpw+V6adf0UKLTUWCNCpR6P54Xj8LttxdrcIK4qmldbrPQbM4v1BYAFIptVoJyq61S5/U4kRrkSpe6UNe0NS/+tX+O0fGmqoxc0MLW3DllAeVCrNRSOQN36BXcYuQc49evmn7T1jQ5fYnRCtQQYXYT0zVjvnyMOHdkxu7Vild5ECgYEA82lTGBhq/qzVRpArDuylp5vMJcNIHmrPQknVmuN2ZWdXn/z2CJ1IncYN9/y7SwIMKTR4r4AhdXjrZ/VSCxlJx/9Jdzq4ezjYQy+y0uIuyrKAju5cLcJB7gKvP+fKvSSog40nul6bRrAK7rzG5PQbkWbXwmhJLaBertKqMJzmDQcCgYEAonPfDCYCWwRWdYKa/x5r6t5v8XJOgY4m5bidlOkqME+U0cbds3utjTrv4Fuv52G8fokl8VlsId+pAUPp17Y/kyCfquWJBDNTJHzn820pmHmqGC+B2Rd7A3Suz8BDX3Su4Eqa/k02C6WQncFiOKGY2SZIDjbrbrUaYyHojLRcT00CgYEAuE8QNDaJDb4g4AVaFTrrVDaMJivv99g4h6EQtO93PuE5YFNSmMM/sPe+rAAacXlja+/nUjxbGXAuD9GaBf1n+KA9KKK1/dfVKUp8xWGuvJzc611hPuTQt0AhIuqIgR+42aOQaXYpbSTvokYe9E5CcZ7F0NbWUk4UY5vwck7bgxECgYBt/7eQKkx+BLnmZ8E77gV0y5lRC+1MnEyKUC+jnR4MCFbklJuZ5KznbHjwmiTJr8xKgeNHQR2O8jsluC6m+aiinvx+Y14bkgVCN7F3ivsXW4ppRgHOVa/d7yxPUAhOolunU90PRIjL6he6Oi+f2Qv+PYWc2mMgW1C6v0MPwzCeIQKBgQCVFguqQ1+DBJz3XjXTqIdgrsMUoJ8qmv7uO2eTkXMwv+mNvQM9R3WUrsaPT1MuS2FGxPVIp7/ODBM1NANxf6JZkERzUf3ASKLu6Y2fjMD0yVjkehTX/6gVfg0Xm1F2kYVntypSoD9ghsnFIA5gTrW7WIAgEHf5Un1CJtJjAChLBQ=="
        });
        ENCODED_PRECALCULATED_VALUES.add(new String[]{
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsiAvdeM2P3u1baHgqE610pkqYWTuhN8Z5jQscLo1ECv4NifwrJ9w4ETcgmM+5GgHx9rJ2IRfT6Mz0UbBacl7jDgH6P494ZrDftmM6BZk7iFRrFSX2J1/6EnHZHrvdSBxvbNuqBB2kHojXcV68QoWAvj53Lu62wlCuFsJEhoVemMjIRgPdtN9VY/owjAzfAW2Y7xJInvu0DkJB697TDnAKkPPOGn0/JHJKu1c317D2+hyR6RmxyHn96catG5aM2jGdnhvqkBjQwahm+62wQ4Pyjn6FLcBxbKbcTpDbtXrdIIKeJRCfpGnOKFmPBvZ8KI8Vkng4DKUKMHGvfz50rpknQIDAQAB",
                "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCyIC914zY/e7VtoeCoTrXSmSphZO6E3xnmNCxwujUQK/g2J/Csn3DgRNyCYz7kaAfH2snYhF9PozPRRsFpyXuMOAfo/j3hmsN+2YzoFmTuIVGsVJfYnX/oScdkeu91IHG9s26oEHaQeiNdxXrxChYC+Pncu7rbCUK4WwkSGhV6YyMhGA92031Vj+jCMDN8BbZjvEkie+7QOQkHr3tMOcAqQ884afT8kckq7VzfXsPb6HJHpGbHIef3pxq0blozaMZ2eG+qQGNDBqGb7rbBDg/KOfoUtwHFsptxOkNu1et0ggp4lEJ+kac4oWY8G9nwojxWSeDgMpQowca9/PnSumSdAgMBAAECggEBAJ13dn1w73QPmMPiZwhjDLmwTZbr8y/jSrIHTYIaGu7A8BxwEoOIL/ES7x0VP3QLw1UaRXXzD48HZ7eKJVGvnFjI77U5jNr0Rf7Ns5M4uOEK83i1D8ee9IQVNP/O7gFNPT/Gb/yDEU2Hq9wpyBVuJSdbkwdfrkAUX7c6QzqNdI9LtdZ6BmbUlb5JUwHTnOklJX2QZAvqq7jmgP/scRJzLMFIDlnglgy36h4eCUe5P6lxMKHR+4KFp1k6Cf4LtFsS65gTo2sBlLFw34+M4YwBkLYvHu+FJ2uhKRuLEoTkOE+E1tdHzNl/+A8I4SIBaPX2lD8qh2ayVhCqilvGPh03bsECgYEA/HUJc+feT3m3W671y5vyEqsg/GxcLo8vGGUK1/6C2Fprx7PXJt7BN5n/NwabY0fRRf/Uow7fMwfRFBQrD8uppFEPY8flhk6LipsR51X4hUJpTnyoolm3f1fR+c/AFU85sKszgb0sRE1NYxvFzTgqgQYCkdrW/4VRukazI3gpDnMCgYEAtKAcDd/b9HBROmEJxUhZJHOUJudoHNqEEEeqShYyfYg0bydTmc+zn2V8ODP0DlgQRUtn2WeEe9E3pYVfRtcit+h4sNSL7ejKeuYVXUV1bE7VLCFVv21bYWYGu2A0UewrsJET9LaX279rjdwZxBSXE6ldRFcK22UCEEF760PrbK8CgYEA0IUO2LhUrApZw5WgmZtl97t3VGdgQGitupWCvmMOygrtaRaHCCkHKJi+70E6yWcBWLL1e/UFsE2tiMsbwkEzDnCRqJXsY+Guaj6pLA2bZt4ywsw+MVKvOLRdz9ujyTYV7XGK6odI7+V1nP3MhDP3D3jaWISL/qOsnJJW1l4iczcCgYEAlJ549uGO9d3EuChIBc+1qUyARlJ5U+RobYzKDROej7OWkS7mltRTk3/JaeU1lufW221kEXofhFFtXAAVmRBDPNdHFxiL2kGE+QJ/6bLRH0DuHdV/3HZ6jfWI7HwIELAmcq2sodnrsaAbOyoy+SeH92GPoD+Oa179B6sZ70Z/W3kCgYAKhCiAWfbjJP8PysS6tALVnOJrGkroS3aro7Ws7iJG0pXxXqJCNQi1kAcitkibt2sChrF2lhoCiiqGqwGLgRyW7VFq+BibEQC8NSBxK/DHo6DpsEiWoBOBcsVbzWodVy0nRvCPLZZoV/FgNoZiWrwbn/RaolVKTrS1r1iiuKahzg=="
        });
        ENCODED_PRECALCULATED_VALUES.add(new String[]{
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgQI70at0oy5nLrrdJloJPjJMlPkSiPzK+djmCkJkyg5AExEIf2wU/+DoLb8vVJvl6sHq+HTA0ViWoJqbEet8nr6PLI+aSNDhAHgV35RoHDBERSm42dEZswEJ2ZvZhfMuYJLDFER9qO9f+qWUpWR8q5fp8LC2M0ofLcUC7yStBDkzKjyfAqehXG+bHyg90HWZkm8iCZ4TWDndJdB0IBBEP7o3M9wuH8kJiaM1L/i1dl761uoVxyf5ANhec9KvT5L9o49ZuxD8rfjHHa23YvhNFF69MOag+/SwOspVeLZynzeAQ7zioYJXjZrNwvLCdRw3lYF20egTbM2EFOqVGX5alQIDAQAB",
                "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCBAjvRq3SjLmcuut0mWgk+MkyU+RKI/Mr52OYKQmTKDkATEQh/bBT/4Ogtvy9Um+Xqwer4dMDRWJagmpsR63yevo8sj5pI0OEAeBXflGgcMERFKbjZ0RmzAQnZm9mF8y5gksMURH2o71/6pZSlZHyrl+nwsLYzSh8txQLvJK0EOTMqPJ8Cp6Fcb5sfKD3QdZmSbyIJnhNYOd0l0HQgEEQ/ujcz3C4fyQmJozUv+LV2XvrW6hXHJ/kA2F5z0q9Pkv2jj1m7EPyt+Mcdrbdi+E0UXr0w5qD79LA6ylV4tnKfN4BDvOKhgleNms3C8sJ1HDeVgXbR6BNszYQU6pUZflqVAgMBAAECggEAOhsVclR1TmJCGywTG4kGDLt+/sJIdObXTT1CL3DEELXmajAL0ciOlMlqCeDIoqUtI1WATbPYfPIXtfKs0Z9tG9rchceQSCe8kAeGYpNnaPrcZQJrvb+Oga+ADkFB8jEbvGed8ez/ZC2c3zng/7WI6Yic18nf5q4F6QmJTskIHJQ4gDSsbBjwwFN6VJc7RBHlJViBoUgMpgE3ksHa27NM7DbM+y9tC6rx/CFBn2g0CH0nRATj2fOye8IvozDmOd+Zgp4XbR8MrE8e0rgCxjiLTQlN5PeMgqZoZRLAfPTcWBVFE3aHClBpKIxT+kRyKi9S7TrgIr63O/UXEe5PmAjjgQKBgQDZU3Uvyj+/l/qTaxbyzFdc6zPBt/KAlKd+bwsfyvjkM7Z22r9n46pUxuAcE/2AyoZDZeDqWto9498zdgEHptfEO4qdwKRAYnjT4kb1VU1O/Hs2YmkGmWCCxovPbO/FsRYikNPZ1fOs0j9jobc5rKWdjnOPFwviFp/y1mRWHB6JRQKBgQCX92DL7K54WUc7kL0m6LyxiGUZxv82kq0npIdJ16GK9qo4vgYr3n1lud7aUDoa1opeqGsM8KKdgmcx2Sng3+z+Tw/en6t1RXK0r6SHeeQ4EKr5AYr2qR1IkcGN3wxOSSl9rWXdTZUxfzU3Lm2+zvACbKXSPgkeCvLK20wKCAmZEQKBgDxKJ9djLz4ypkQhUFN5Fr9jTI9wPWkoVDMCET73qwZ7xiHA25qxkP5F0cRYU1pYVNj7uWKqY6MJxVDcarq5sV/x+Kl82NQ3vTdirba/vFuuNWxH7sTy5dBBzmVz3iykzPQ3412qOhh2lzlHkrIAUE2eSDao+RX2mtbeXSV6VYpVAoGAMdz55e/DT7n7pY/YgOvc/mPCyLKDC2UVa+yQd6pJV0+YiwXPAJYAj2BtvzST3DqJLIYWmihbM1OWiMS4+RCAsvE+Q84gdFpVSPRZCBr2x26wqwPWlRjogudQmzyUzRs0gghjZDoQui0DSRfy6qj8F8+OmW3BkBoHkIhkauY4QAECgYByEemoH0FKyCJsr5iWxQTTElPh+z6Ts6ke+21QlURDWJJM39cWG4E1BqdZVoCy9FDpB9paVnFK/612vyVBWjt3p4nvjgsNaI3WnFBS5iA6+llkhgEuhVWH5B741qi+pn9zxu6s/coYnJyoEDVSHUL4a+HOPLniIQMcGBwe/v7lOw=="
        });
        ENCODED_PRECALCULATED_VALUES.add(new String[]{
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjhLngAwxxVXQnuiYY2rve4K9X4We0eIWZ9St1KYzizMqg3rbSbMFxxTFeAwVx3VSWHyGZvHoQPEvGR/F6Jbm/6ZS7qVay7OxD7tIZB5BeqaLGpqka14dZTlzWPJZrO24UboHHk+wrRT69NB7oQj9hyr5WOcETOl1viQl631T/ggDXHQtJ7tSQuF7evMin2cBH2WiizMlRhBcjpGsLu1tH0fosd0ebrLabvSN4FCeslNGsbAQQTi/XWQdUF8ySKDxt9+Kyv024o+GGKOsT4yqkKW6/ld/GQHgXXs6kOHkAcf6bUD1YBowdNvARZ4C6mWeZ2+rFtmo7XBz0H3l+/NO7wIDAQAB",
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCOEueADDHFVdCe6Jhjau97gr1fhZ7R4hZn1K3UpjOLMyqDettJswXHFMV4DBXHdVJYfIZm8ehA8S8ZH8Xolub/plLupVrLs7EPu0hkHkF6posamqRrXh1lOXNY8lms7bhRugceT7CtFPr00HuhCP2HKvlY5wRM6XW+JCXrfVP+CANcdC0nu1JC4Xt68yKfZwEfZaKLMyVGEFyOkawu7W0fR+ix3R5ustpu9I3gUJ6yU0axsBBBOL9dZB1QXzJIoPG334rK/Tbij4YYo6xPjKqQpbr+V38ZAeBdezqQ4eQBx/ptQPVgGjB028BFngLqZZ5nb6sW2ajtcHPQfeX7807vAgMBAAECggEAEZZzhB1+V9B8x/9BRiVpyh9wNBAFjjPv1CC2+Dku7k+CNs6RsPjxgSioHWlZlNyIOh4IQVw3BTsWOoT9agKsS/+aCqL7r3XqOGlV19cLAhT16XA1ZHk9KFfJWLx/r3GqPKvLz8vLslGDk6TveQr037PJNGE83gyUn9o3u4RDTpId73pTsoZAYBMV/aDRQn0YTrnuDyWmPAoitEjfwME5093QbiZCluCODO28uC4ioaP7NaJHRuHSC2i1gi7MjVY3z94A7bzR1DVz2gRDQimlY/EQp3V6545JhjVBW82OIq4YLgmWNJrdmK+9na6ZrIezVIVjF9Za5904L8dw9PvceQKBgQDSK07rtvJyc20yeUu5PFRcLCKl+drbde9ZvJ+jAvN2FE0vfc1iHHvbnvJSch6pjZiKJQBsv+8evm68i/UibHCAzk+LmlEVOdVURgGxZfpOvLGvw1cm+0WGCHboXWFg6/MxJdSTvHQVbjYt44nKUpZEb/3fdAX1G2t3r+ApbvG8awKBgQCtDizSuKKqSLPtmknDFhG0Qnyss48WCs7qMcoGF1cwiCqlIs+gI4qd1LeN4moJF8XZLnMWvHr7DF1yWK2gyOKfnCpmAI9eHcDZxYCmbZkP8+9a43cqjPVmhyniZwhrGLoYgfbFWENtzph/sIw1+EqLN0GXuN6InZqVCiEzNo2YjQKBgEhjjFpZ+CX9pdXlu3RA2FmrBgAfEAfZFijVdDKkeJDqKy+5Z/1sDCk4FQTas774u1qRphTPAyCWvPOKTVOfAB0Nco6GB3mFIvpU5o5grpdiHN2KLcGKeIbS8YnVaA4HFwuNmSuNlHoxLM4fej1KtuE5pbrKbqbe2+QsmswKn46jAoGBAJbl91u/N8f4ITyb/pmn0sQ1/XSyFaCIyCgaRijNjW96LIVWN2lR1HsPkwabWQ6YtiJMiHpY23uxKoKcpLnAsSuwBqBu+JR+qTy7JRT6GC9660l7ctudkhZW4cTTRtnr88mO+djWv+Ut8nfVQE2HNajzA7UCtLi3hKVU8eIwPYO5AoGABek/CF5iohYeOy0FbhhxIW75iQVMOtBOZREsvcqzhkv0Nn8wiByib8n/rsGPNq9JPuaYnuPN/i4cG2ooq6XZOhcNAiedWXdEUTmT/H3TdHaNXyCu46aaTfJ02jPXTjAqIIgwXGzY1t6MGy/wJ23r8Yz/25dQAITaejl7xgi6ofw="
        });
    }

    @Override
    public byte[] generateRaw(SecretTypeEnum type, String annotationName, int size) {
        LOG.info("Generating deterministic {} data for key '{}' of size: {}", type, annotationName, size);
        return super.generateRaw(type, annotationName, size);
    }

    @Override
    public Object[] generatePairedRaw(SecretTypeEnum type, String annotationName, int size, String userId) {
        LOG.info("Using pre-generated value for {}", annotationName);
        String rootAnnotationName = Parameter.extractBareParameterName(annotationName);
        String[] values = ENCODED_PRECALCULATED_VALUES.get(
                Math.abs(rootAnnotationName.hashCode()) % ENCODED_PRECALCULATED_VALUES.size());

        if (SecretTypeEnum.GPG == type) {
            final byte[] password = super.generateRaw(SecretTypeEnum.PASSWORD, annotationName, 128);
            /*
             * Use the deterministic RSA keys to generate GPG secret key. These are encoded, so decode prior.
             */
            final GPGKeyGenerator gpgKeyGenerator = new GPGKeyGenerator();
            final GPGKeyGenerator.GPGKeyPair gpgKeyPair = gpgKeyGenerator.generateDeterministicKeyPair(
                    userId, new String(password), DECODER.decode(values[0]), DECODER.decode(values[1]));
            return new Object[]{gpgKeyPair.getPublicKey(), gpgKeyPair.getPrivateKey(), password};
        }

        return new Object[]{
                DECODER.decode(values[0]), DECODER.decode(values[1])
        };
    }

    @Override
    protected SecureRandom getRandomFromKey(String key) {
        //only seed based on the name without the field selector, so RSA keys use the same seed
        String comparisonKey = Parameter.extractBareParameterName(key);
        InsecureRandom random = new InsecureRandom();
        random.setSeed(comparisonKey.hashCode());
        return random;
    }
}
